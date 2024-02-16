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

class CordicTop(val mantissaBits: Int, val fractionBits: Int, val iterations: Int)
    extends Module {
  val io = IO(CordicTopIO(mantissaBits + fractionBits))

  val preprocessor  = Module(new TrigFuncPreprocessor(mantissaBits, fractionBits, iterations))
  val postprocessor = Module(new TrigFuncPostprocessor(mantissaBits, fractionBits, iterations))
  val cordicCore    = Module(new CordicCore(mantissaBits, fractionBits, iterations))

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
      cordicOps: Seq[String] = Seq(),
      mantissaBits: Int = 0,
      fractionBits: Int = 0,
      iterations: Int = 0
  )

  val builder = OParser.builder[Config]

  val parser1 = {
    import builder._
    OParser.sequence(
      programName("CordicTop"),
      opt[String]('t', "target-dir")
        .action((x, c) => c.copy(td = x))
        .text("Verilog target directory"),
      opt[Seq[String]]('c', "cordic_ops")
        .valueName("<op1>,<op1>...")
        .text("Cordic operations")
        .action((x, c) => c.copy(cordicOps = x)),
      opt[Int]('m', "mantissa_bits")
        .text("Number of mantissa bits used")
        .action((x, c) => c.copy(mantissaBits = x)),
      opt[Int]('f', "fraction_bits")
        .text("Number of fraction bits used")
        .action((x, c) => c.copy(fractionBits = x)),
      opt[Int]('i', "iterations")
        .text("How many iterations CORDIC runs")
        .action((x, c) => c.copy(iterations = x))
    )
  }

  OParser.parse(parser1, args, Config()) match {
    case Some(config) => {

      val opList = config.cordicOps.map(className => {
        val packageName   = this.getClass.getPackage.getName
        val classInstance = Class.forName(packageName + ".Cordic" + className)
        val constructor   = classInstance.getConstructor(classOf[Int], classOf[Int], classOf[Int])
        val instance      = constructor.newInstance(config.mantissaBits, config.fractionBits, config.iterations)
        instance.asInstanceOf[CordicOp]
      })
      // These lines generate the Verilog output
      (new ChiselStage).execute(
        {Array("-td", config.td) },
        Seq(
          ChiselGeneratorAnnotation(() => {
            new CordicTop(
              config.mantissaBits,
              config.fractionBits,
              config.iterations,
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
