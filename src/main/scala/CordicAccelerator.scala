// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicAccelerator
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-06
package accelerators

import chisel3._
import chisel3.util.{ValidIO, log2Ceil}
import circt.stage.{ChiselStage, FirtoolOption}
import chisel3.stage.ChiselGeneratorAnnotation
import scopt.OParser
import java.io.File

case class CordicAcceleratorIO(dataWidth: Int) extends Bundle {

  val in = Input(ValidIO(new Bundle {
    val rs1 = SInt(dataWidth.W)
    val rs2 = SInt(dataWidth.W)
    val rs3 = SInt(dataWidth.W)
    val op     = UInt(5.W)
  }))

  val out = Output(ValidIO(new Bundle {
    val dataOut = SInt(dataWidth.W)
  }))

}

class CordicAccelerator(val mantissaBits: Int, val fractionBits: Int, val iterations: Int, opList: Seq[CordicOp])
    extends Module {
  val io = IO(CordicAcceleratorIO(mantissaBits + fractionBits))

  val preprocessor  = Module(new CordicPreprocessor(mantissaBits, fractionBits, iterations, opList))
  val postprocessor = Module(new CordicPostprocessor(mantissaBits, fractionBits, iterations, opList))
  val cordicCore    = Module(new CordicCore(mantissaBits, fractionBits, iterations))

  preprocessor.io.in.rs1 := io.in.bits.rs1
  preprocessor.io.in.rs2 := io.in.bits.rs2
  preprocessor.io.in.rs3 := io.in.bits.rs3
  preprocessor.io.in.op  := io.in.bits.op

  cordicCore.io.in.bits  := preprocessor.io.out
  cordicCore.io.in.valid := io.in.valid

  postprocessor.io.in.cordic  := cordicCore.io.out.bits.cordic
  postprocessor.io.in.control := cordicCore.io.out.bits.control

  io.out.bits.dataOut := postprocessor.io.out.dOut
  io.out.valid        := cordicCore.io.out.valid

}

object CordicAccelerator extends App {
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
      programName("CordicAccelerator"),
      opt[String]('t', "target-dir")
        .action((x, c) => c.copy(td = x))
        .text("Verilog target directory"),
      opt[Seq[String]]('c', "cordic_ops")
        .valueName("<op1>,<op1>...")
        .text("Cordic operations")
        .action( (x, c) => c.copy(cordicOps = x)),
      opt[Int]('m', "mantissa_bits")
        .text("Number of mantissa bits used")
        .action( (x, c) => c.copy(mantissaBits = x)),
      opt[Int]('f', "fraction_bits")
        .text("Number of fraction bits used")
        .action( (x, c) => c.copy(fractionBits = x)),
      opt[Int]('i', "iterations")
        .text("How many iterations CORDIC runs")
        .action( (x, c) => c.copy(iterations = x)))
  }

  OParser.parse(parser1, args, Config()) match {
    case Some(config) => {

      val opList = config.cordicOps.map(className => {
        val packageName = this.getClass.getPackage.getName
        val classInstance = Class.forName(packageName + ".Cordic" + className)
        val constructor = classInstance.getConstructor(classOf[Int], classOf[Int], classOf[Int])
        val instance = constructor.newInstance(config.mantissaBits, config.fractionBits, config.iterations)
        instance.asInstanceOf[CordicOp]
      })
      // These lines generate the Verilog output
      (new circt.stage.ChiselStage).execute(
        { Array("--target", "systemverilog") ++ Array("-td", config.td) },
        Seq(
          ChiselGeneratorAnnotation(() => {
            new CordicAccelerator(
              config.mantissaBits,
              config.fractionBits,
              config.iterations,
              opList
            )
          }),
          FirtoolOption("--disable-all-randomization")
        )
      )
    }
    case _ => {
      println("Could not parse arguments")
    }
  }
}
