// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

/**
  * Basic postprocessor that simply moves in.cordic.x/y/z to out.cordic.x/y/z. 
  * dOut is constant 0.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class BasicPostprocessor(mantissaBits: Int, fractionBits: Int,
                         iterations: Int)
  extends CordicPostprocessor(mantissaBits, fractionBits, iterations) {

  io.out.cordic <> io.in.cordic
  io.out.dOut   := 0.S

}