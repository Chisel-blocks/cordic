// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

object TrigOp extends ChiselEnum {
  val SINE        = Value(0.U)
  val COSINE      = Value(1.U)
  val ARCTAN      = Value(2.U)
  val SINH        = Value(3.U)
  val COSH        = Value(4.U)
  val ARCTANH     = Value(5.U)
  val EXPONENTIAL = Value(6.U)
  val LOG         = Value(7.U)
}


object TrigFuncControl {
  /** Larger than pi over 2 */
  val LTPO2 = 0
  /** Smaller than -pi over 2 */
  val STNPO2 = 1
}

/**
  * Preprocessor for trigonometric operation support.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class TrigFuncPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations, repr) {

  val largerThanPiOver2     = io.in.bits.rs1 > consts.pPiOver2
  val smallerThanNegPiOver2 = io.in.bits.rs1 < consts.nPiOver2

  // Adder needed for
  // Rescaling Sin and Cos to improve range
  // Generating inputs for Log

  val addA = WireDefault(0.S)
  val addB = WireDefault(0.S)
  val subA = WireDefault(0.S)
  val subB = WireDefault(0.S)

  // Mux for adder input
  when ((io.in.bits.control === TrigOp.SINE.asUInt) || (io.in.bits.control === TrigOp.COSINE.asUInt)) {
    addA := io.in.bits.rs1
    addB := consts.pPi
    subA := io.in.bits.rs1
    subB := consts.pPi
  } .elsewhen (io.in.bits.control === TrigOp.LOG.asUInt) {
    addA := io.in.bits.rs1
    addB := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits, repr)
    subA := io.in.bits.rs1
    subB := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits, repr)
  }

  val adder = addA + addB
  val subtractor = subA - subB

  when (io.in.bits.control === TrigOp.SINE.asUInt) {
    io.out.bits.cordic.x := consts.K
    io.out.bits.cordic.y := 0.S
    io.out.bits.cordic.z := MuxCase(io.in.bits.rs1, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
    io.out.bits.control.rotType := CordicRotationType.CIRCULAR
    io.out.bits.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.bits.control === TrigOp.COSINE.asUInt) {
    io.out.bits.cordic.x := consts.K
    io.out.bits.cordic.y := 0.S
    io.out.bits.cordic.z := MuxCase(io.in.bits.rs1, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
    io.out.bits.control.rotType := CordicRotationType.CIRCULAR
    io.out.bits.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.bits.control === TrigOp.ARCTAN.asUInt) {
    io.out.bits.cordic.x := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits, repr)
    io.out.bits.cordic.y := io.in.bits.rs1
    io.out.bits.cordic.z := 0.S
    io.out.bits.control.rotType := CordicRotationType.CIRCULAR
    io.out.bits.control.mode := CordicMode.VECTORING
  } .elsewhen (io.in.bits.control === TrigOp.SINH.asUInt) {
    io.out.bits.cordic.x := consts.Kh
    io.out.bits.cordic.y := 0.S
    io.out.bits.cordic.z := io.in.bits.rs1
    io.out.bits.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.bits.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.bits.control === TrigOp.COSH.asUInt) {
    io.out.bits.cordic.x := consts.Kh
    io.out.bits.cordic.y := 0.S
    io.out.bits.cordic.z := io.in.bits.rs1
    io.out.bits.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.bits.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.bits.control === TrigOp.ARCTANH.asUInt) {
    io.out.bits.cordic.x := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits, repr)
    io.out.bits.cordic.y := io.in.bits.rs1
    io.out.bits.cordic.z := 0.S
    io.out.bits.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.bits.control.mode := CordicMode.VECTORING
  } .elsewhen (io.in.bits.control === TrigOp.EXPONENTIAL.asUInt) {
    io.out.bits.cordic.x := consts.Kh
    io.out.bits.cordic.y := consts.Kh
    io.out.bits.cordic.z := io.in.bits.rs1
    io.out.bits.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.bits.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.bits.control === TrigOp.LOG.asUInt) {
    io.out.bits.cordic.x := adder
    io.out.bits.cordic.y := subtractor
    io.out.bits.cordic.z := 0.S
    io.out.bits.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.bits.control.mode := CordicMode.VECTORING
  } .otherwise {
    io.out.bits.cordic.x := DontCare
    io.out.bits.cordic.y := DontCare
    io.out.bits.cordic.z := DontCare
    io.out.bits.control.rotType := CordicRotationType.CIRCULAR
    io.out.bits.control.mode := CordicMode.ROTATION
  }

  io.out.bits.control.custom := Cat(io.in.bits.control, largerThanPiOver2, smallerThanNegPiOver2)

  io.out.valid := io.in.valid
  io.in.ready  := io.out.ready

}

