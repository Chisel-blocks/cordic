// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

class BasicPostprocessor(mantissaBits: Int, fractionBits: Int,
                         iterations: Int)
  extends CordicPostprocessor(mantissaBits, fractionBits, iterations) {

  io.out.cordic <> io.in.cordic
  io.out.dOut   := 0.S

}