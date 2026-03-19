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
  * @param config UpConvertConfig
  */
class UpConvertPreprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String, config: UpConvertConfig)
  extends CordicPreprocessor(mantissaBits, fractionBits, iterations, repr) {

  val phaseAccum = RegInit(0.S(config.phaseAccumWidth.W))
  // Control word sets the frequency
  val controlWord = io.in.bits.control.asSInt

  if (config.usePhaseAccum) {
    when (io.out.fire) {
      phaseAccum := phaseAccum + controlWord
    }
  }

  // Use MSB bits for phase value
  val phase: SInt = if (config.usePhaseAccum) {
    val widthDiff = config.phaseAccumWidth - (mantissaBits + fractionBits)
    if (widthDiff >= 0) {
      phaseAccum.head(mantissaBits + fractionBits).asSInt
    } else {
      (phaseAccum << (-widthDiff)).asSInt
    }
  } else {
    io.in.bits.rs3
  }

  // TODO: check if it should be >= or <= for efficient hw
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

  val rs1_neg = ~io.in.bits.rs1 + 1.S
  val rs2_neg = ~io.in.bits.rs2 + 1.S

  io.out.bits.cordic.x := MuxCase(io.in.bits.rs1, Seq(largerThanPiOver2 -> rs2_neg, smallerThanNegPiOver2 -> io.in.bits.rs2))
  io.out.bits.cordic.y := MuxCase(io.in.bits.rs2, Seq(largerThanPiOver2 -> io.in.bits.rs1, smallerThanNegPiOver2 -> rs1_neg))
  io.out.bits.cordic.z := MuxCase(phase, Seq(largerThanPiOver2 -> subtractor, smallerThanNegPiOver2 -> adder))
  io.out.bits.control.rotType := CordicRotationType.CIRCULAR
  io.out.bits.control.mode := CordicMode.ROTATION

  io.out.bits.control.custom := 0.U

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready

}
