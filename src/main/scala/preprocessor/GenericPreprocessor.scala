// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

case class GenericControls() extends Bundle {
  val rot_type = CordicRotationType()
  val mode     = CordicMode()
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

  val control = io.in.bits.control.asTypeOf(GenericControls())
  val phase = io.in.bits.rs3

  val isCircular = control.rot_type === CordicRotationType.CIRCULAR
  val largerThanPiOver2     = isCircular && (phase > consts.pPiOver2)
  val smallerThanNegPiOver2 = isCircular && (phase < consts.nPiOver2)

  val rs1_neg = ~io.in.bits.rs1 + 1.S
  val rs2_neg = ~io.in.bits.rs2 + 1.S
  val new_phase = phase + MuxCase(0.S, Seq(largerThanPiOver2 -> consts.nPiOver2, smallerThanNegPiOver2 -> consts.pPiOver2))

  io.out.bits.cordic.x := MuxCase(io.in.bits.rs1, Seq(largerThanPiOver2 -> rs2_neg       , smallerThanNegPiOver2 -> io.in.bits.rs2))
  io.out.bits.cordic.y := MuxCase(io.in.bits.rs2, Seq(largerThanPiOver2 -> io.in.bits.rs1, smallerThanNegPiOver2 -> rs1_neg))
  io.out.bits.cordic.z := new_phase

  io.out.bits.control.rotType := control.rot_type
  io.out.bits.control.mode    := control.mode
  io.out.bits.control.custom  := 0.U

  io.out.valid := io.in.valid
  io.in.ready  := io.out.ready

}

