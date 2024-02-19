// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

/**
  * Preprocessor for usign CORDIC for upconversion
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class UpConvertPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations, repr) {

  val largerThanPiOver2     = io.in.rs3 > consts.pPiOver2
  val smallerThanNegPiOver2 = io.in.rs3 < consts.nPiOver2

  // Adder needed for
  // applying a +- 90 degree pre-rotation if needed

  val addA = WireDefault(0.S)
  val addB = WireDefault(0.S)
  val subA = WireDefault(0.S)
  val subB = WireDefault(0.S)

  addA := io.in.rs3
  addB := consts.pPiOver2
  subA := io.in.rs3
  subB := consts.pPiOver2

  val adder = addA + addB
  val subtractor = subA - subB

  val rs1_neg = ~io.in.rs1 + 1.S
  val rs2_neg = ~io.in.rs2 + 1.S

  io.out.cordic.x := MuxCase(io.in.rs1, Seq(largerThanPiOver2 -> rs2_neg, smallerThanNegPiOver2 -> io.in.rs2))
  io.out.cordic.y := MuxCase(io.in.rs2, Seq(largerThanPiOver2 -> io.in.rs1, smallerThanNegPiOver2 -> rs1_neg))
  io.out.cordic.z := MuxCase(io.in.rs3, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
  io.out.control.rotType := CordicRotationType.CIRCULAR
  io.out.control.mode := CordicMode.ROTATION

  io.out.control.custom := 0.U

}
