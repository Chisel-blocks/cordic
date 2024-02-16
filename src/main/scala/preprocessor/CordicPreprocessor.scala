// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicPreprocessor
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-11
package cordic

import chisel3._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicPreprocessorIO(dataWidth: Int) extends Bundle {

  val in = new Bundle {
    /** Source register 1 */
    val rs1     = Input(SInt(dataWidth.W))
    /** Source register 2 */
    val rs2     = Input(SInt(dataWidth.W))
    /** Source register 3 */
    val rs3     = Input(SInt(dataWidth.W))
    /** Control bits */
    val control = Input(UInt())
  }

  val out = new Bundle {
    /** Outputs to CordicCore */
    val cordic  = Output(CordicBundle(dataWidth))
    /** Control bundle */
    val control = Output(CordicCoreControl())
  }

}

/**
  * Abstract class for Cordic Preprocessor that defines parameters and IOs
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
abstract class CordicPreprocessor(val mantissaBits: Int, val fractionBits: Int,
                                  val iterations: Int) extends Module {
  val io = IO(CordicPreprocessorIO(dataWidth = mantissaBits + fractionBits))
  val consts = CordicConstants(mantissaBits, fractionBits, iterations)
}


object CordicPreprocessor extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array() ++ args },
    Seq(
      ChiselGeneratorAnnotation(() =>
        new TrigFuncPreprocessor(
          4,
          12,
          14,
        )
      ),
    )
  )

}
