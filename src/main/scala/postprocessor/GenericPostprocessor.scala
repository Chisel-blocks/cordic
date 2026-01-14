// SPDX-License-Identifier: Apache-2.0

package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

/**
  * Basic postprocessor that simply moves in.cordic.x/y/z to out.cordic.x/y/z. 
  * dOut is constant 0.
  *
  * @param mantissaBits
  * @param fractionBits
  * @param iterations
  */
class GenericPostprocessor(mantissaBits: Int, fractionBits: Int,
                         iterations: Int, repr: String)
  extends CordicPostprocessor(mantissaBits, fractionBits, iterations, repr) {

  val control = io.in.bits.control.asTypeOf(CordicGenericControls())

  val scaledCordicOut = Wire(Vec(3, SInt(16.W)))

  for (i <- 0 until 3) {
    val cordicOut = {
      if (i == 0)      io.in.bits.cordic.x
      else if (i == 1) io.in.bits.cordic.y
      else             io.in.bits.cordic.z
    }
    val mulResult = Wire(SInt(32.W))
    when (control.out_mul(i) === OutputMultiplier.ONE_OVER_K) {
      mulResult := cordicOut * consts.K
    } .elsewhen (control.out_mul(i) === OutputMultiplier.ONE_OVER_KH) {
      mulResult := cordicOut * consts.Kh
    } .otherwise {
      mulResult := 0.S
    }
    scaledCordicOut(i) := mulResult >> 16
  }

  io.out.bits.cordic.x := scaledCordicOut(0)
  io.out.bits.cordic.y := scaledCordicOut(1)
  io.out.bits.cordic.z := scaledCordicOut(2)

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready

  // Not used
  io.out.bits.dOut   := 0.S

}