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

class TrigFuncPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations) {

  val largerThanPiOver2     = io.in.rs1 > consts.pPiOver2
  val smallerThanNegPiOver2 = io.in.rs1 < consts.nPiOver2

  val addA = WireDefault(0.S)
  val addB = WireDefault(0.S)
  val subA = WireDefault(0.S)
  val subB = WireDefault(0.S)

  when ((io.in.control === TrigOp.SINE.asUInt) || (io.in.control === TrigOp.COSINE.asUInt)) {
    addA := io.in.rs1
    addB := consts.pPi
    subA := io.in.rs1
    subB := consts.pPi
  } .elsewhen (io.in.control === TrigOp.LOG.asUInt) {
    addA := io.in.rs1
    addB := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits)
    subA := io.in.rs1
    subB := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits)
  }

  val adder = addA + addB
  val subtractor = subA - subB

  when (io.in.control === TrigOp.SINE.asUInt) {
    io.out.cordic.x := consts.K
    io.out.cordic.y := 0.S
    io.out.cordic.z := MuxCase(io.in.rs1, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.control === TrigOp.COSINE.asUInt) {
    io.out.cordic.x := consts.K
    io.out.cordic.y := 0.S
    io.out.cordic.z := MuxCase(io.in.rs1, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.control === TrigOp.ARCTAN.asUInt) {
    io.out.cordic.x := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits)
    io.out.cordic.y := io.in.rs1
    io.out.cordic.z := 0.S
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.VECTORING
  } .elsewhen (io.in.control === TrigOp.SINH.asUInt) {
    io.out.cordic.x := consts.Kh
    io.out.cordic.y := 0.S
    io.out.cordic.z := io.in.rs1
    io.out.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.control === TrigOp.COSH.asUInt) {
    io.out.cordic.x := consts.Kh
    io.out.cordic.y := 0.S
    io.out.cordic.z := io.in.rs1
    io.out.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.control === TrigOp.ARCTANH.asUInt) {
    io.out.cordic.x := CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits)
    io.out.cordic.y := io.in.rs1
    io.out.cordic.z := 0.S
    io.out.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.control.mode := CordicMode.VECTORING
  } .elsewhen (io.in.control === TrigOp.EXPONENTIAL.asUInt) {
    io.out.cordic.x := consts.Kh
    io.out.cordic.y := consts.Kh
    io.out.cordic.z := io.in.rs1
    io.out.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.control === TrigOp.LOG.asUInt) {
    io.out.cordic.x := adder
    io.out.cordic.y := subtractor
    io.out.cordic.z := 0.S
    io.out.control.rotType := CordicRotationType.HYPERBOLIC
    io.out.control.mode := CordicMode.VECTORING
  } .otherwise {
    io.out.cordic.x := DontCare
    io.out.cordic.y := DontCare
    io.out.cordic.z := DontCare
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.ROTATION
  }

  io.out.control.custom := Cat(io.in.control, largerThanPiOver2, smallerThanNegPiOver2)

}

