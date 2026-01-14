// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

object InputSel extends ChiselEnum {
  val X = Value(0.U)  
  val Y = Value(1.U)
  val Z = Value(2.U)
}

object OutputMultiplier extends ChiselEnum {
  val ONE = Value(0.U)
  val ONE_OVER_K = Value(1.U)
  val ONE_OVER_KH = Value(2.U)
}

case class CordicGenericControls(bitwidth: Int) extends Bundle {
  val rot_type  = CordicRotationType()
  val mode      = CordicMode()
  val in_sel    = Vec(3, InputSel())
  val out_sel   = Vec(3, InputSel())
  val out_mul   = Vec(3, OutputMultiplier())
  val adder_ops = Vec(3, SInt(bitwidth.W))
}

/**
  * Generic Preprocessor that performs prerotation for circular modes.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class GenericPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations, repr) {

  val control = io.in.bits.control.asTypeOf(CordicGenericControls(mantissaBits+fractionBits))

  val rs1_int = MuxCase(io.in.bits.rs1, Seq(
    (control.in_sel(0) === InputSel.Y) -> io.in.bits.rs2,
    (control.in_sel(0) === InputSel.Z) -> io.in.bits.rs3))
  val rs2_int = MuxCase(io.in.bits.rs1, Seq(
    (control.in_sel(1) === InputSel.Y) -> io.in.bits.rs2,
    (control.in_sel(1) === InputSel.Z) -> io.in.bits.rs3))
  val rs3_int = MuxCase(io.in.bits.rs1, Seq(
    (control.in_sel(2) === InputSel.Y) -> io.in.bits.rs2,
    (control.in_sel(2) === InputSel.Z) -> io.in.bits.rs3))

  val rs1 = rs1_int + control.adder_ops(0)
  val rs2 = rs2_int + control.adder_ops(1)
  val rs3 = rs3_int + control.adder_ops(2)

  val phase = rs3

  val isCircular = control.rot_type === CordicRotationType.CIRCULAR
  val largerThanPiOver2     = isCircular && (phase > consts.pPiOver2)
  val smallerThanNegPiOver2 = isCircular && (phase < consts.nPiOver2)

  val rs1_neg = ~rs1_int + 1.S + control.adder_ops(0)
  val rs2_neg = ~rs2_int + 1.S + control.adder_ops(1)
  val new_phase = phase + MuxCase(0.S, Seq(largerThanPiOver2 -> consts.nPiOver2, smallerThanNegPiOver2 -> consts.pPiOver2))

  io.out.bits.cordic.x := MuxCase(rs1, Seq(largerThanPiOver2 -> rs2_neg , smallerThanNegPiOver2 -> rs2))
  io.out.bits.cordic.y := MuxCase(rs2, Seq(largerThanPiOver2 -> rs1     , smallerThanNegPiOver2 -> rs1_neg))
  io.out.bits.cordic.z := new_phase

  io.out.bits.control.rotType := control.rot_type
  io.out.bits.control.mode    := control.mode
  io.out.bits.control.custom  := control.asUInt

  io.out.valid := io.in.valid
  io.in.ready  := io.out.ready

}

