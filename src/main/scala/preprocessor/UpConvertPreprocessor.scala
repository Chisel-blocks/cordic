// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation
import cordic.config.UpConvertConfig

/**
  * Preprocessor for usign CORDIC for upconversion
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  * @param repr "fixed-point" or "pi"
  * @param usePhaseAccum use internal phase accumulator (requires repr = "pi")
  */
class UpConvertPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String, config: UpConvertConfig)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations, repr) {

  val phaseAccum = RegInit(0.S(config.phaseAccumWidth.W))
  // Control word sets the frequency
  val controlWord = io.in.control.asSInt

  if (config.usePhaseAccum) {
    when (io.in.valid) {
      phaseAccum := phaseAccum + controlWord
    }
  }

  // Use MSB bits for phase value
  val phase = {
    if (config.usePhaseAccum) phaseAccum.head(mantissaBits + fractionBits).asSInt
    else io.in.rs3
  }

  val largerThanPiOver2     = phase > consts.pPiOver2
  val smallerThanNegPiOver2 = phase < consts.nPiOver2

  // Adder needed for
  // applying a +- 90 degree pre-rotation if needed

  val addA = WireDefault(0.S)
  val addB = WireDefault(0.S)
  val subA = WireDefault(0.S)
  val subB = WireDefault(0.S)

  addA := phase
  addB := consts.pPiOver2
  subA := phase
  subB := consts.pPiOver2

  val adder = addA + addB
  val subtractor = subA - subB

  val rs1_neg = ~io.in.rs1 + 1.S
  val rs2_neg = ~io.in.rs2 + 1.S

  io.out.cordic.x := MuxCase(io.in.rs1, Seq(largerThanPiOver2 -> rs2_neg, smallerThanNegPiOver2 -> io.in.rs2))
  io.out.cordic.y := MuxCase(io.in.rs2, Seq(largerThanPiOver2 -> io.in.rs1, smallerThanNegPiOver2 -> rs1_neg))
  io.out.cordic.z := MuxCase(phase, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
  io.out.control.rotType := CordicRotationType.CIRCULAR
  io.out.control.mode := CordicMode.ROTATION

  io.out.control.custom := 0.U

}
