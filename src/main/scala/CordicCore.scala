package accelerators

import chisel3._
import chisel3.util.{ValidIO, RegEnable, Fill, Cat}
import circt.stage.{ChiselStage, FirtoolOption}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicCoreIO(dataWidth: Int) extends Bundle {

  val in = Input(ValidIO(new Bundle {
    val cordic  = CordicBundle(dataWidth)
    val control = CordicCoreControl()
  }))

  val out = Output(ValidIO(CordicBundle(dataWidth)))
}

class CordicCore(mantissaBits: Int, fractionBits: Int, iterations: Int) extends Module {
  val io = IO(CordicCoreIO(dataWidth = mantissaBits + fractionBits))

  val wordLen = mantissaBits + fractionBits
  val highBit = wordLen - 1

  val nRepeats = {
    var n = 0
    for (i <- 0 until iterations) {
      if (CordicConstants.hyperbolicRepeatIndices.contains(i)) {
        n += 1
      }
    }
    n
  }

  val totalIterations = iterations + nRepeats

  val inRegs = RegEnable(io.in, io.in.valid)

  val adders = Seq.fill(totalIterations)(Seq.fill(3)(Module(new AdderSubtractor(mantissaBits + fractionBits))))

  val inWires      = Seq.fill(totalIterations)(Wire(chiselTypeOf(io.in)))
  val outWires     = Seq.fill(totalIterations)(Wire(chiselTypeOf(io.in)))
  val pipelineRegs = Seq.tabulate(totalIterations)(i => RegEnable(outWires(i), outWires(i).valid))
  inWires(0) := inRegs

  val LUT = CordicLut(mantissaBits, fractionBits, totalIterations)

  var repeats = 0
  var repeat  = false
  var shiftIdx = 0
  var lutIdx = 0

  for (i <- 0 until totalIterations) {

    val m     = Wire(Bool())
    val sigma = Wire(Bool())
    when(inWires(i).bits.control.mode === CordicMode.ROTATION) {
      sigma := inWires(i).bits.cordic.z >= 0.S
    }.otherwise { // VECTORING
      sigma := inWires(i).bits.cordic.y < 0.S
    }

    when(inWires(i).bits.control.rotType === CordicRotationType.CIRCULAR) {
      m := true.B
    }.otherwise { // HYPERBOLIC
      m := false.B
    }

    if (i > 0) {
      inWires(i) := pipelineRegs(i - 1)
    }

    if (CordicConstants.hyperbolicRepeatIndices.contains(i - repeats) && !repeat) {
      repeat = true
      repeats += 1
    } else {
      repeat = false
    }

    val bypass = WireDefault(false.B)
    if (i == 0) {
      when(m === 0.U) {
        bypass := true.B
      }
    }

    val signExt = Wire(CordicBundle(mantissaBits + fractionBits))
    val xSelect = ~(m ^ sigma)
    val ySelect = ~sigma
    val zSelect = sigma

    // Shift and sign extend x and y
    signExt.x := inWires(i).bits.cordic.x >> shiftIdx
    signExt.y := inWires(i).bits.cordic.y >> shiftIdx
    signExt.z := 0.S

    // Add or subtract
    adders(i)(0).io.A := inWires(i).bits.cordic.x
    adders(i)(0).io.B := signExt.y
    adders(i)(0).io.D := xSelect
    adders(i)(1).io.A := inWires(i).bits.cordic.y
    adders(i)(1).io.B := signExt.x
    adders(i)(1).io.D := ySelect
    adders(i)(2).io.A := inWires(i).bits.cordic.z
    adders(i)(2).io.B := Mux(m.asBool, LUT.atanVals(lutIdx), LUT.atanhVals(lutIdx))
    adders(i)(2).io.D := zSelect

    when(bypass) {
      outWires(i) := inWires(i)
    }.otherwise {
      outWires(i).bits.cordic.x := adders(i)(0).io.S
      outWires(i).bits.cordic.y := adders(i)(1).io.S
      outWires(i).bits.cordic.z := adders(i)(2).io.S
      outWires(i).bits.control  := inWires(i).bits.control
      outWires(i).valid         := inWires(i).valid
    }

    if (!repeat) {
      shiftIdx += 1
      lutIdx += 1
    }
  }

  io.out.bits  := pipelineRegs(iterations - 1).bits.cordic
  io.out.valid := pipelineRegs(iterations - 1).valid
}

object CordicCore extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array("--target", "systemverilog") ++ args },
    Seq(ChiselGeneratorAnnotation(() => new CordicCore(4, 12, 14)), FirtoolOption("--disable-all-randomization"))
  )

}
