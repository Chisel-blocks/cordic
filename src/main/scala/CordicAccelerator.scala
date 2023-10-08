// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicAccelerator
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-06
package accelerators

import chisel3._
import chisel3.util._

case class CordicAcceleratorIO(dataWidth: Int) extends Bundle {
  val dataIn  = Input(UInt(dataWidth.W))
  val op      = Input(TrigonometricOp())
  val dataOut = Output(UInt(dataWidth.W))
}

class CordicAccelerator(mantissaBits: Int, fractionBits: Int) extends Module {
  val io = CordicAcceleratorIO(dataWidth = mantissaBits + fractionBits)
}

