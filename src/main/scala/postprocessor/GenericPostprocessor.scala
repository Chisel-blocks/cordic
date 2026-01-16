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

  val control = io.in.bits.control.asTypeOf(CordicGenericControls(mantissaBits+fractionBits))

  val scaledCordicOut = Wire(Vec(3, SInt(16.W)))

  // Multiply by 1/K or 1/Kh to scale output
  for (i <- 0 until 3) {
    val cordicOut = {
      if (i == 0)      io.in.bits.cordic.x
      else if (i == 1) io.in.bits.cordic.y
      else             io.in.bits.cordic.z
    }
    val mulResult = Wire(SInt(32.W))
    when (control.out_mul(i) === OutputMultiplier.ONE_OVER_K) {
      mulResult := cordicOut * consts.KTimesPi
    } .elsewhen (control.out_mul(i) === OutputMultiplier.ONE_OVER_KH) {
      mulResult := cordicOut * consts.KhTimesPi
    } .otherwise {
      mulResult := 0.S
    }
    scaledCordicOut(i) := mulResult >> 16
  }

  io.out.bits.cordic.x := MuxCase(scaledCordicOut(0), Seq(
    (control.out_sel(0) === InputSel.Y) -> scaledCordicOut(1),
    (control.out_sel(0) === InputSel.Z) -> scaledCordicOut(2)
  ))
  io.out.bits.cordic.y := MuxCase(scaledCordicOut(0), Seq(
    (control.out_sel(1) === InputSel.Y) -> scaledCordicOut(1),
    (control.out_sel(1) === InputSel.Z) -> scaledCordicOut(2)
  ))
  io.out.bits.cordic.z := MuxCase(scaledCordicOut(0), Seq(
    (control.out_sel(2) === InputSel.Y) -> scaledCordicOut(1),
    (control.out_sel(2) === InputSel.Z) -> scaledCordicOut(2)
  ))

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready

  // Not used
  io.out.bits.dOut   := 0.S

}