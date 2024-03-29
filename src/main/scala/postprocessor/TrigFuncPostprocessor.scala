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
  val rescale = io.in.control.custom(TrigFuncControl.LTPO2) || io.in.control.custom(TrigFuncControl.STNPO2)

  // Either x or y needs to be rescaled, depending on SINE or COSINE operation
  val adderA = WireDefault(0.S)
  when (io.in.control.custom(31,2) === TrigOp.SINE.asUInt) {
    adderA := ~io.in.cordic.y
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.COSINE.asUInt) {
    adderA := ~io.in.cordic.x
  }

  // Rescale from negative to positive, or vice versa
  val rescaled = adderA + 1.S

  when (io.in.control.custom(31,2) === TrigOp.SINE.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := Mux(rescale, rescaled, io.in.cordic.y)
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.y
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.COSINE.asUInt) {
    io.out.cordic.x := Mux(rescale, rescaled, io.in.cordic.x)
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.x
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.ARCTAN.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.z
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.SINH.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.y
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.COSH.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.x
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.ARCTANH.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.z
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.EXPONENTIAL.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    io.out.dOut     := io.out.cordic.x
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.LOG.asUInt) {
    io.out.cordic.x := io.in.cordic.x
    io.out.cordic.y := io.in.cordic.y
    io.out.cordic.z := io.in.cordic.z
    // CORDIC returns 0.5*log - this multiplies by 2
    io.out.dOut     := io.out.cordic.z << 1
  } .otherwise {
    io.out.cordic.x := DontCare
    io.out.cordic.y := DontCare
    io.out.cordic.z := DontCare
    io.out.dOut     := DontCare
  }
}
