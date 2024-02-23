package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

import amba.common._
import amba.axi4l._

class AXI4LCordic(
  mantissaBits: Int,
  fractionBits: Int,
  iterations: Int,
  preprocessorClass: String,
  postprocessorClass: String,
  val addr_width: Int = 32,
  val data_width: Int = 32,
)  extends Module with AXI4LSlaveLogic with AXI4LSlave {

  val cordic = Module(new CordicTop(mantissaBits,
                                    fractionBits,
                                    iterations,
                                    preprocessorClass,
                                    postprocessorClass))

  val inRegs = Reg(Vec(3, UInt(32.W)))
  val outRegs = Reg(Vec(3, UInt(32.W)))
  cordic.io.in.bits.rs1 := inRegs(0).asSInt
  cordic.io.in.bits.rs2 := inRegs(1).asSInt
  cordic.io.in.bits.rs3 := inRegs(2).asSInt
  outRegs(0) := cordic.io.out.bits.cordic.x.asUInt
  outRegs(1) := cordic.io.out.bits.cordic.y.asUInt
  outRegs(2) := cordic.io.out.bits.cordic.z.asUInt
  // Memory map for input and output registers
  val memory_map = Map[Data, MemoryRange](
    inRegs(0) -> MemoryRange(begin = 0, end = 3, mode = MemoryMode.W, name = "x_in"),
    inRegs(1) -> MemoryRange(begin = 4, end = 7, mode = MemoryMode.W, name = "y_in"),
    inRegs(2) -> MemoryRange(begin = 8, end = 11, mode = MemoryMode.W, name = "z_in"),
    outRegs(0) -> MemoryRange(begin = 12, end = 15, mode = MemoryMode.R, name = "x_out"),
    outRegs(1) -> MemoryRange(begin = 16, end = 19, mode = MemoryMode.R, name = "y_out"),
    outRegs(2) -> MemoryRange(begin = 20, end = 23, mode = MemoryMode.R, name = "z_out"),
    cordic.io.in.bits.control -> MemoryRange(begin = 24, end = 27, mode = MemoryMode.RW, name = "control"),
    cordic.io.in.valid -> MemoryRange(begin = 28, end = 31, mode = MemoryMode.RW, name = "control_2"),
  )

  buildAxi4LiteSlaveLogic()
}

object AXI4LCordic extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array() ++ args },
    Seq(ChiselGeneratorAnnotation(() => new AXI4LCordic(3, 14, 16, "Basic", "Basic", 32, 32)))
  )

}