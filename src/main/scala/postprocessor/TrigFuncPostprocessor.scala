// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

/**
  * Postprocessor for trigonometric operations.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class TrigFuncPostprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int, repr: String)
  extends CordicPostprocessor(mantissaBits, fractionBits, iterations, repr) {

  // Rescaling needed if it was performed in perprocessor
  val rescale = io.in.bits.control.custom(TrigFuncControl.LTPO2) || io.in.bits.control.custom(TrigFuncControl.STNPO2)

  // Either x or y needs to be rescaled, depending on SINE or COSINE operation
  val adderA = WireDefault(0.S)
  when (io.in.bits.control.custom(31,2) === TrigOp.SINE.asUInt) {
    adderA := ~io.in.bits.cordic.y
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.COSINE.asUInt) {
    adderA := ~io.in.bits.cordic.x
  }

  // Rescale from negative to positive, or vice versa
  val rescaled = adderA + 1.S

  when (io.in.bits.control.custom(31,2) === TrigOp.SINE.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := Mux(rescale, rescaled, io.in.bits.cordic.y)
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.y
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.COSINE.asUInt) {
    io.out.bits.cordic.x := Mux(rescale, rescaled, io.in.bits.cordic.x)
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.x
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.ARCTAN.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.z
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.SINH.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.y
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.COSH.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.x
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.ARCTANH.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.z
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.EXPONENTIAL.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    io.out.bits.dOut     := io.out.bits.cordic.x
  } .elsewhen(io.in.bits.control.custom(31,2) === TrigOp.LOG.asUInt) {
    io.out.bits.cordic.x := io.in.bits.cordic.x
    io.out.bits.cordic.y := io.in.bits.cordic.y
    io.out.bits.cordic.z := io.in.bits.cordic.z
    // CORDIC returns 0.5*log - this multiplies by 2
    io.out.bits.dOut     := io.out.bits.cordic.z << 1
  } .otherwise {
    io.out.bits.cordic.x := DontCare
    io.out.bits.cordic.y := DontCare
    io.out.bits.cordic.z := DontCare
    io.out.bits.dOut     := DontCare
  }

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
