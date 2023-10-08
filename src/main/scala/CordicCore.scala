package accelerators

import chisel3._
import chisel3.util.{ValidIO, RegEnable}
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

  val inRegs = RegEnable(io.in, io.in.valid)

}

object CordicCoreMain extends App {
  // These lines generate the Verilog output
  (new ChiselStage).execute(
    Array("--target", "systemverilog"),
    Seq(ChiselGeneratorAnnotation(() => new CordicCore(4, 12, 14)),
      FirtoolOption("--disable-all-randomization"))
  )
}
