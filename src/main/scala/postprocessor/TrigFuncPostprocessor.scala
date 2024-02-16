// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

class TrigFuncPostprocessor(mantissaBits: Int, fractionBits: Int,
                           iterations: Int)
  extends CordicPostprocessor(mantissaBits, fractionBits, iterations) {

  val rescale = io.in.control.custom(TrigFuncControl.LTPO2) || io.in.control.custom(TrigFuncControl.STNPO2)

  val adderA = WireDefault(0.S)
  when (io.in.control.custom(31,2) === TrigOp.SINE.asUInt) {
    adderA := ~io.in.cordic.y
  } .elsewhen(io.in.control.custom(31,2) === TrigOp.COSINE.asUInt) {
    adderA := ~io.in.cordic.x
  }

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
  } .otherwise {
    io.out.cordic.x := DontCare
    io.out.cordic.y := DontCare
    io.out.cordic.z := DontCare
    io.out.dOut     := DontCare
  }
}
