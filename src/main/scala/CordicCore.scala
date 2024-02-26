package cordic

import chisel3._
import chisel3.util.{ValidIO, RegEnable, Fill, Cat}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicCoreIO(dataWidth: Int) extends Bundle {

  val in = Input(ValidIO(new Bundle {
    val cordic  = CordicBundle(dataWidth)
    val control = CordicCoreControl()
  }))

  val out = Output(ValidIO(new Bundle {
    val cordic  = CordicBundle(dataWidth)
    val control = CordicCoreControl()
  }))

}

/**
  * Configurable Cordic core for rotation/vectoring circular/hyperbolic. 
  * User can select which modes are generated. 
  * The generator minimizes hardware for cases when some mode or type is not needed.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  * @param repr Number representation for atan values
  * @param enableCircular
  * @param enableHyperbolic
  * @param enableRotational
  * @param enableVectoring
  */
class CordicCore(mantissaBits: Int,
                 fractionBits: Int,
                 iterations: Int,
                 repr: String,
                 enableCircular: Boolean,
                 enableHyperbolic: Boolean,
                 enableRotational: Boolean,
                 enableVectoring: Boolean
                 ) extends Module {
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

  val totalIterations = if (enableHyperbolic) iterations + nRepeats
                        else                  iterations

  val inRegs     = RegEnable(io.in.bits, io.in.valid)
  val inValidReg = RegInit(false.B)
  inValidReg := io.in.valid

  val adders = Seq.fill(totalIterations)(Seq.fill(3)(Module(new AdderSubtractor(mantissaBits + fractionBits))))

  val inWires      = Seq.fill(totalIterations)(Wire(chiselTypeOf(io.in)))
  val outWires     = Seq.fill(totalIterations)(Wire(chiselTypeOf(io.in)))
  val pipelineRegs = Seq.tabulate(totalIterations)(i => RegEnable(outWires(i).bits, outWires(i).valid))
  val validRegs    = Seq.tabulate(totalIterations)(i => RegInit(false.B))

  validRegs.zip(outWires).map { case (validReg, outWire) => validReg := outWire.valid }
  inWires(0).bits  := inRegs
  inWires(0).valid := inValidReg

  // Initialize all valid signals as false

  val LUT = CordicLut(mantissaBits, fractionBits, totalIterations, repr)

  var repeats  = 0
  var repeat   = false
  var shiftIdx = 0
  var lutIdx   = 0

  for (i <- 0 until totalIterations) {

    
    // Generate control logic only if needed
    // If only either mode is enabled, no need to check for mode
    val rotationalSelect = {
      if      (enableRotational && enableVectoring)  inWires(i).bits.control.mode === CordicMode.ROTATION
      else if (enableRotational && !enableVectoring) true.B
      else false.B
    }
    
    val vectoringSelect = {
      if      (enableVectoring && enableRotational)  inWires(i).bits.control.mode === CordicMode.VECTORING
      else if (enableVectoring && !enableRotational) true.B
      else false.B
    }
    
    val sigma = WireDefault(false.B)
    when(rotationalSelect) {
      sigma := inWires(i).bits.cordic.z >= 0.S
    }
    when(vectoringSelect) {
      sigma := inWires(i).bits.cordic.y < 0.S
    }

    // Generate control logic only if needed
    // If only either type is enabled, no need to check for type
    val circularSelect = {
      if      (enableCircular && enableHyperbolic)  inWires(i).bits.control.rotType === CordicRotationType.CIRCULAR
      else if (enableCircular && !enableHyperbolic) true.B
      else false.B
    }

    val hyperbolicSelect = {
      if      (enableHyperbolic && enableRotational)  inWires(i).bits.control.rotType === CordicRotationType.HYPERBOLIC
      else if (enableHyperbolic && !enableRotational) true.B
      else false.B
    }

    val m = WireDefault(false.B)

    when(circularSelect) {
      m := true.B
    }
    when(hyperbolicSelect) {
      m := false.B
    }

    if (i > 0) {
      inWires(i).bits  := pipelineRegs(i - 1)
      inWires(i).valid := validRegs(i - 1)
    }

    // Keep track of repeat iterations required for hyperbolic mode
    if (CordicConstants.hyperbolicRepeatIndices.contains(i - repeats) && !repeat && enableHyperbolic) {
      repeat = true
      repeats += 1
    } else {
      repeat = false
    }

    // First stage should be bypassed for hyperbolic mode
    val bypass = WireDefault(false.B)
    if (i == 0 && enableHyperbolic) {
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

  io.out.bits  := pipelineRegs(totalIterations - 1)
  io.out.valid := validRegs(totalIterations - 1)
}

object CordicCore extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array() ++ args },
    Seq(ChiselGeneratorAnnotation(() => new CordicCore(4, 12, 14,
                                                       "fixed-point", true, true, true, true)))
  )

}
