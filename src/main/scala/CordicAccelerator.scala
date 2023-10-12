// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicAccelerator
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-06
package accelerators

import chisel3._
import chisel3.util.{ValidIO, log2Ceil}
import circt.stage.{ChiselStage, FirtoolOption}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicAcceleratorIO(dataWidth: Int) extends Bundle {

  val in = Input(ValidIO(new Bundle {
    val dataIn = SInt(dataWidth.W)
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

  preprocessor.io.in.rs1 := io.in.bits.dataIn
  preprocessor.io.in.rs2 := 0.S
  preprocessor.io.in.rs3 := 0.S
  preprocessor.io.in.op  := io.in.bits.op

  cordicCore.io.in.bits  := preprocessor.io.out
  cordicCore.io.in.valid := io.in.valid

  postprocessor.io.in.cordic  := cordicCore.io.out.bits.cordic
  postprocessor.io.in.control := cordicCore.io.out.bits.control

  io.out.bits.dataOut := postprocessor.io.out.dOut
  io.out.valid        := cordicCore.io.out.valid

}

object CordicAccelerator extends App {

  // These lines generate the Verilog output
  (new circt.stage.ChiselStage).execute(
    { Array("--target", "systemverilog") ++ args },
    Seq(
      ChiselGeneratorAnnotation(() => {
        val mantissaBits = 4
        val fractionBits = 12
        val iterations   = 14
        new CordicAccelerator(
          mantissaBits,
          fractionBits,
          iterations,
          Seq(
            CordicSine(mantissaBits, fractionBits, iterations),
            CordicCosine(mantissaBits, fractionBits, iterations),
            CordicArctan(mantissaBits, fractionBits, iterations),
            CordicSinh(mantissaBits, fractionBits, iterations),
            CordicCosh(mantissaBits, fractionBits, iterations),
            CordicArctanh(mantissaBits, fractionBits, iterations),
            CordicExponential(mantissaBits, fractionBits, iterations),
            CordicLog(mantissaBits, fractionBits, iterations)
          )
        )
      }),
      FirtoolOption("--disable-all-randomization")
    )
  )

}
