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

  val addPi = io.in.rs1 + consts.pPi
  val subPi = io.in.rs1 - consts.pPi

  when (io.in.control === TrigOp.SINE.asUInt) {
    io.out.cordic.x := consts.K
    io.out.cordic.y := 0.S
    io.out.cordic.z := MuxCase(io.in.rs1, Seq(largerThanPiOver2 -> subPi, smallerThanNegPiOver2 -> addPi))
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.ROTATION
  } .elsewhen (io.in.control === TrigOp.COSINE.asUInt) {
    io.out.cordic.x := consts.K
    io.out.cordic.y := 0.S
    io.out.cordic.z := MuxCase(io.in.rs1, Seq(largerThanPiOver2 -> subPi, smallerThanNegPiOver2 -> addPi))
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.ROTATION
  } .otherwise {
    io.out.cordic.x := DontCare
    io.out.cordic.y := DontCare
    io.out.cordic.z := DontCare
    io.out.control.rotType := CordicRotationType.CIRCULAR
    io.out.control.mode := CordicMode.ROTATION
  }

  io.out.control.custom := Cat(io.in.control, largerThanPiOver2, smallerThanNegPiOver2)

}

