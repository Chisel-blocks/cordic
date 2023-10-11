// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicPreprocessor
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-11
package accelerators

import chisel3._
import chisel3.util._

case class CordicPreprocessorIO(dataWidth: Int) extends Bundle {
  val dataIn  = Input(UInt(dataWidth.W))
  val op      = Input(TrigonometricOp())
  val mode    = Output(CordicMode())
  val rotType = Output(CordicRotationType())
}

class CordicPreprocessor(val mantissaBits: Int, val fractionBits: Int, val iterations: Int, opList: Seq[CordicOp])
    extends Module with CordicInfo {
  val io = CordicPreprocessorIO(dataWidth = mantissaBits + fractionBits)
}
