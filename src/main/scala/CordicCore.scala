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
  val inRegs  = RegEnable(io.in, io.in.valid)
  io.out.valid  := false.B
  io.out.bits.x := 0.U
  io.out.bits.y := 0.U
  io.out.bits.z := 0.U

  val adders = Seq.fill(iterations)(Seq.fill(3)(Module(new AdderSubtractor(mantissaBits + fractionBits))))

  val inWires      = Seq.fill(iterations)(Wire(CordicBundle(mantissaBits + fractionBits)))
  val signExtWires = Seq.fill(iterations)(Wire(CordicBundle(mantissaBits + fractionBits)))
  inWires(0) := inRegs.bits.cordic

  val pipelineRegs = Seq.fill(iterations)(Reg(CordicBundle(mantissaBits + fractionBits)))

  val LUT = CordicLut(mantissaBits, fractionBits, iterations)

  for (i <- 0 until iterations) {
    // Shift and sign extend x, y, and z
    signExtWires(i).x := (inWires(i).x >> i) | Cat(Mux(
      inWires(i).x(highBit),
      Fill(i, 1.U),
      Fill(i, 0.U)
    ), Fill(wordLen - i, 0.U)) 
    signExtWires(i).y := (inWires(i).y >> i) | Cat(Mux(
      inWires(i).y(highBit),
      Fill(i, 1.U),
      Fill(i, 0.U)
    ), Fill(wordLen - i, 0.U))
    signExtWires(i).z := (inWires(i).z >> i) | Cat(Mux(
      inWires(i).z(highBit),
      Fill(i, 1.U),
      Fill(i, 0.U)
    ), Fill(wordLen - i, 0.U))


    // Add or subtract 
    adders(i)(0).io.A := inWires(i).x
    adders(i)(0).io.B := signExtWires(i).y
    // adders(i)(0).io.D := TODO
    adders(i)(1).io.A := inWires(i).y
    adders(i)(1).io.B := signExtWires(i).x
    // adders(i)(1).io.D := TODO
    adders(i)(2).io.A := inWires(i).z
    adders(i)(2).io.B := LUT.atanVals(i)
    // adders(i)(2).io.D := TODO

  }

}

object CordicCoreMain extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array("--target", "systemverilog") ++ args },
    Seq(ChiselGeneratorAnnotation(() => new CordicCore(4, 12, 14)), FirtoolOption("--disable-all-randomization"))
  )

}
