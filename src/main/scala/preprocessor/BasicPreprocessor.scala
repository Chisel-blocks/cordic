// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

class BasicPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations) {

  io.out.cordic.x := io.in.rs1
  io.out.cordic.y := io.in.rs2
  io.out.cordic.z := io.in.rs3

  io.out.control.rotType := Mux(io.in.control(0), CordicRotationType.HYPERBOLIC, CordicRotationType.CIRCULAR)
  io.out.control.mode    := Mux(io.in.control(1), CordicMode.VECTORING, CordicMode.ROTATION)
  io.out.control.custom  := 0.U

}

