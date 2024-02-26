// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicTop
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-06
package cordic

import chisel3._
import chisel3.util.{ValidIO, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation
import scopt.OParser
import java.io.File
import chisel3.util.RegEnable
import cordic.config._

case class CordicTopIO(dataWidth: Int) extends Bundle {

  val in = Input(ValidIO(new Bundle {
    val rs1     = SInt(dataWidth.W)
    val rs2     = SInt(dataWidth.W)
    val rs3     = SInt(dataWidth.W)
    val control = UInt(32.W)
  }))

  val out = Output(ValidIO(new Bundle {
    val cordic = CordicBundle(dataWidth)
    val dOut   = SInt(dataWidth.W)
  }))

}

class CordicTop
  (val config: CordicConfig)
    extends Module {
  
  val mantissaBits = config.mantissaBits
  val fractionBits = config.fractionBits
  val iterations = config.iterations
  val preprocessorClass = config.preprocessorClass
  val postprocessorClass = config.postprocessorClass

  val io = IO(CordicTopIO(mantissaBits + fractionBits))

  val preprocessor: CordicPreprocessor  = {
    if (preprocessorClass == "Basic")          Module(new BasicPreprocessor(mantissaBits, fractionBits, iterations, "fixed-point"))
    else if (preprocessorClass == "TrigFunc")  Module(new TrigFuncPreprocessor(mantissaBits, fractionBits, iterations, "fixed-point"))
    else if (preprocessorClass == "UpConvert") Module(new UpConvertPreprocessor(mantissaBits, fractionBits, iterations, "pi", config.upConvertConfig.get))
    else {
      throw new RuntimeException(s"Illegal type for preprocessorClass: $preprocessorClass")
      Module(new BasicPreprocessor(mantissaBits, fractionBits, iterations, "fixed-point"))
    }
  }
  val postprocessor: CordicPostprocessor  = {
    if (postprocessorClass == "Basic")          Module(new BasicPostprocessor(mantissaBits, fractionBits, iterations, preprocessor.repr))
    else if (postprocessorClass == "TrigFunc")  Module(new TrigFuncPostprocessor(mantissaBits, fractionBits, iterations, preprocessor.repr))
    else if (postprocessorClass == "UpConvert") Module(new BasicPostprocessor(mantissaBits, fractionBits, iterations, preprocessor.repr))
    else {
      throw new RuntimeException(s"Illegal type for postprocessorClass: $postprocessorClass")
      Module(new BasicPostprocessor(mantissaBits, fractionBits, iterations, "fixed-point"))
    }
  }

  val cordicCore    = Module(new CordicCore(mantissaBits, fractionBits, iterations, repr = preprocessor.repr))

  val inRegs      = RegEnable(io.in.bits, io.in.valid)
  val outRegs     = RegEnable(postprocessor.io.out, cordicCore.io.out.valid)
  val inValidReg  = RegInit(false.B)
  val outValidReg = RegInit(false.B)
  inValidReg  := io.in.valid
  outValidReg := io.out.valid

  preprocessor.io.in.rs1     := inRegs.rs1
  preprocessor.io.in.rs2     := inRegs.rs2
  preprocessor.io.in.rs3     := inRegs.rs3
  preprocessor.io.in.control := inRegs.control
  preprocessor.io.in.valid   := inValidReg

  cordicCore.io.in.bits  := preprocessor.io.out
  cordicCore.io.in.valid := inValidReg

  postprocessor.io.in.cordic  := cordicCore.io.out.bits.cordic
  postprocessor.io.in.control := cordicCore.io.out.bits.control

  outRegs.cordic  := postprocessor.io.out.cordic
  outRegs.dOut    := postprocessor.io.out.dOut
  outValidReg     := cordicCore.io.out.valid

  io.out.bits  := outRegs
  io.out.valid := outValidReg

}

object CordicTop extends App {

  case class Config(
      td: String = ".",
      config: String = ""
  )

  val builder = OParser.builder[Config]

  val parser1 = {
    import builder._
    OParser.sequence(
      programName("CordicTop"),
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
            new CordicTop(
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
