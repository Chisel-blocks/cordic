// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicPostprocessor
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-12
package cordic

import chisel3._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicPostprocessorIO(dataWidth: Int) extends Bundle {

  val in = new Bundle {
    val cordic  = Input(CordicBundle(dataWidth))
    val control = Input(CordicCoreControl())
  }

  val out = new Bundle {

    /** Bundle of post processed x, y, and z */
    val cordic = Output(CordicBundle(dataWidth))

    /** Additional output signal to be freely used */
    val dOut = Output(SInt(dataWidth.W))
  }

}

/**
  * Abstract class for Cordic postprocessors. Defines IOs and input parameters.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  * @param repr
  */
abstract class CordicPostprocessor(val mantissaBits: Int, val fractionBits: Int,
                                   val iterations: Int, val repr: String = "fixed-point") 
  extends Module {

  val io = IO(CordicPostprocessorIO(dataWidth = mantissaBits + fractionBits))
  val consts = CordicConstants(mantissaBits, fractionBits, iterations, repr)
}

object CordicPostprocessor extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array() ++ args },
    Seq(
      ChiselGeneratorAnnotation(() =>
        new TrigFuncPostprocessor(
          4,
          12,
          14,
          "fixed-point"
        )
      ),
    )
  )

}
