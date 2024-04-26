// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicTop
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-06
package cordic

import chisel3._
import chisel3.util.{ValidIO, log2Ceil, RegEnable}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation
import chisel3.experimental.ExtModule
import scopt.OParser
import java.io.File
import cordic.config._

trait hasCordicTopIO {
  def io: CordicTopIO
}

case class CordicTopIO(
  dataWidth: Int,
  useIn1: Boolean,
  useIn2: Boolean,
  useIn3: Boolean,
  useOut1: Boolean,
  useOut2: Boolean,
  useOut3: Boolean,
  useDout: Boolean
  ) extends Bundle {

  val in = Input(ValidIO(new Bundle {
    val rs1     = if (useIn1) Some(SInt(dataWidth.W)) else None
    val rs2     = if (useIn2) Some(SInt(dataWidth.W)) else None
    val rs3     = if (useIn3) Some(SInt(dataWidth.W)) else None
    val control = UInt(32.W)
  }))

  val out = Output(ValidIO(new Bundle {
    val cordic = new Bundle {
      val x = if (useOut1) Some(SInt(dataWidth.W)) else None
      val y = if (useOut2) Some(SInt(dataWidth.W)) else None
      val z = if (useOut3) Some(SInt(dataWidth.W)) else None
    }
    val dOut   = if (useDout) Some(SInt(dataWidth.W)) else None
  }))

}

class CordicBlackBox(config: CordicConfig) extends ExtModule with hasCordicTopIO {
  override val desiredName = "cordic"
  val io = IO(CordicTopIO(
    dataWidth = config.mantissaBits + config.fractionBits,
    useIn1 = config.usedInputs.contains(1),
    useIn2 = config.usedInputs.contains(2),
    useIn3 = config.usedInputs.contains(3),
    useOut1 = config.usedOutputs.contains(1),
    useOut2 = config.usedOutputs.contains(2),
    useOut3 = config.usedOutputs.contains(3),
    useDout = config.useDout
  ))
  val clock = IO(Input(Bool()))
  val reset = IO(Input(Bool()))
}

class CordicTop
  (val config: CordicConfig)
    extends Module with hasCordicTopIO {

  override def desiredName = "cordic"

  val mantissaBits = config.mantissaBits
  val fractionBits = config.fractionBits
  val iterations = config.iterations
  val repr = config.numberRepr
  val preprocessorClass = config.preprocessorClass
  val postprocessorClass = config.postprocessorClass

  val io = IO(CordicTopIO(dataWidth = mantissaBits + fractionBits,
                          useIn1 = config.usedInputs.contains(1),
                          useIn2 = config.usedInputs.contains(2),
                          useIn3 = config.usedInputs.contains(3),
                          useOut1 = config.usedOutputs.contains(1),
                          useOut2 = config.usedOutputs.contains(2),
                          useOut3 = config.usedOutputs.contains(3),
                          useDout = config.useDout))

  val preprocessor: CordicPreprocessor  = {
    if      (preprocessorClass == "Basic")     Module(new BasicPreprocessor(mantissaBits, fractionBits, iterations, repr))
    else if (preprocessorClass == "TrigFunc")  Module(new TrigFuncPreprocessor(mantissaBits, fractionBits, iterations, repr))
    else if (preprocessorClass == "UpConvert") Module(new UpConvertPreprocessor(mantissaBits, fractionBits, iterations, repr, config.upConvertConfig.get))
    else throw new RuntimeException(s"Illegal type for preprocessorClass: $preprocessorClass")
  }
  val postprocessor: CordicPostprocessor  = {
    if      (postprocessorClass == "Basic")     Module(new BasicPostprocessor(mantissaBits, fractionBits, iterations, repr))
    else if (postprocessorClass == "TrigFunc")  Module(new TrigFuncPostprocessor(mantissaBits, fractionBits, iterations, repr))
    else if (postprocessorClass == "UpConvert") Module(new BasicPostprocessor(mantissaBits, fractionBits, iterations, repr))
    else throw new RuntimeException(s"Illegal type for postprocessorClass: $postprocessorClass")
  }

  val cordicCore = Module(new CordicCore(mantissaBits,
                                         fractionBits,
                                         iterations,
                                         preprocessor.repr,
                                         config.enableCircular,
                                         config.enableHyperbolic,
                                         config.enableRotational,
                                         config.enableVectoring))

  val inRegs      = RegEnable(io.in.bits, io.in.valid)
  val outRegs     = RegEnable(postprocessor.io.out, cordicCore.io.out.valid)
  val inValidReg  = RegInit(false.B)
  val outValidReg = RegInit(false.B)
  inValidReg  := io.in.valid
  outValidReg := io.out.valid

  preprocessor.io.in.rs1     := inRegs.rs1.getOrElse(0.S)
  preprocessor.io.in.rs2     := inRegs.rs2.getOrElse(0.S)
  preprocessor.io.in.rs3     := inRegs.rs3.getOrElse(0.S)
  preprocessor.io.in.control := inRegs.control
  preprocessor.io.in.valid   := inValidReg

  cordicCore.io.in.bits  := preprocessor.io.out
  cordicCore.io.in.valid := inValidReg

  postprocessor.io.in.cordic  := cordicCore.io.out.bits.cordic
  postprocessor.io.in.control := cordicCore.io.out.bits.control

  outRegs.cordic  := postprocessor.io.out.cordic
  outRegs.dOut    := postprocessor.io.out.dOut
  outValidReg     := cordicCore.io.out.valid

  if(config.usedOutputs.contains(1)) io.out.bits.cordic.x.get := outRegs.cordic.x
  if(config.usedOutputs.contains(2)) io.out.bits.cordic.y.get := outRegs.cordic.y
  if(config.usedOutputs.contains(3)) io.out.bits.cordic.z.get := outRegs.cordic.z
  if(config.useDout)                 io.out.bits.dOut.get     := outRegs.dOut

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
      programName("cordic"),
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
