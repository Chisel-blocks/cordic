package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.stage.{ChiselStage, ChiselGeneratorAnnotation}

import cordic.config._
import amba.common._
import amba.axi4l._
import cordic.config._
import scopt.OParser

class AXI4LCordic(
  val config: CordicConfig,
  val addr_width: Int = 32,
  val data_width: Int = 32,
)  extends Module with AXI4LSlaveLogic with AXI4LSlave {

  val cordic = Module(new CordicTop(config))

  val inRegs = Reg(Vec(2, UInt(32.W)))
  val outRegs = Reg(Vec(3, UInt(32.W)))
  cordic.io.in.bits.rs1 := inRegs(0)(15,0).asSInt
  cordic.io.in.bits.rs2 := inRegs(0)(31,16).asSInt
  cordic.io.in.bits.rs3 := inRegs(1).asSInt
  outRegs(0) := cordic.io.out.bits.cordic.x.asUInt
  outRegs(1) := cordic.io.out.bits.cordic.y.asUInt
  outRegs(2) := cordic.io.out.bits.cordic.z.asUInt
  // Memory map for input and output registers
  val memory_map = Map[Data, MemoryRange](
    inRegs(0) -> MemoryRange(begin = 0, end = 3, mode = MemoryMode.W, name = "x_y_in"),
    inRegs(1) -> MemoryRange(begin = 4, end = 7, mode = MemoryMode.W, name = "z_in"),
    outRegs(0) -> MemoryRange(begin = 8, end = 11, mode = MemoryMode.R, name = "x_out", stall_sig = !cordic.io.out.valid),
    outRegs(1) -> MemoryRange(begin = 12, end = 15, mode = MemoryMode.R, name = "y_out", stall_sig = !cordic.io.out.valid),
    outRegs(2) -> MemoryRange(begin = 16, end = 19, mode = MemoryMode.R, name = "z_out", stall_sig = !cordic.io.out.valid),
    cordic.io.in.bits.control -> MemoryRange(begin = 20, end = 23, mode = MemoryMode.RW, name = "control"),
    cordic.io.in.valid -> MemoryRange(begin = 24, end = 27, mode = MemoryMode.RW, name = "valid_in"),
    cordic.io.out.valid -> MemoryRange(begin = 28, end = 31, mode = MemoryMode.R, name = "valid_out"),
  )

  buildAxi4LiteSlaveLogic()
}

object AXI4LCordic extends App {

  case class Config(
      td: String = ".",
      config: String = ""
  )

  val builder = OParser.builder[Config]

  val parser1 = {
    import builder._
    OParser.sequence(
      programName("AXI4LCordic"),
      opt[String]('t', "target_dir")
        .action((x, c) => c.copy(td = x))
        .text("Verilog target directory"),
      opt[String]('f', "config_file")
        .text("path to config YAML file")
        .action((x, c) => c.copy(config = x)),
    )
  }

  OParser.parse(parser1, args, Config()) match {
    case Some(config) => {

      val cordic_config = CordicConfig.loadFromFile(config.config)

      // These lines generate the Verilog output
      (new ChiselStage).execute(
        {Array("-td", config.td) },
        Seq(
          ChiselGeneratorAnnotation(() => {
            new AXI4LCordic(
              cordic_config
            )
          }),
        )
      )
    }
    case _ => {
      println("Could not parse arguments")
    }
  }

}