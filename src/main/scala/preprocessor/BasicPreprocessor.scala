// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

/**
  * Basic preprocessor that expects x_0, y_0, z_0 to be in rs1, rs2, and rs3, respectively. 
  * Control bit 0 -> Circular mode (0), Hyperbolic mode (1). 
  * Control bit 1 -> Rotation mode (0), Vectoring mode (1).
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class BasicPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations, repr) {

  io.out.cordic.x := io.in.rs1
  io.out.cordic.y := io.in.rs2
  io.out.cordic.z := io.in.rs3

  io.out.control.rotType := Mux(io.in.control(0), CordicRotationType.HYPERBOLIC, CordicRotationType.CIRCULAR)
  io.out.control.mode    := Mux(io.in.control(1), CordicMode.VECTORING, CordicMode.ROTATION)
  io.out.control.custom  := 0.U

}

