// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicPostprocessor
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-12
package accelerators

import chisel3._
import chisel3.experimental._
import chisel3.util.{MuxCase, log2Ceil}
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicPostprocessorIO(dataWidth: Int) extends Bundle {

  val in = new Bundle {
    val cordic  = Input(CordicBundle(dataWidth))
    val control = Input(CordicCoreControl())
  }

  val out = new Bundle {

    /** Bundle of post processed x, y, and z */
    val cordic = Output(CordicBundle(dataWidth))

    /** Signal indicated by opList.resReg */
    val dOut = Output(SInt(dataWidth.W))
  }

}

class CordicPostprocessor(val mantissaBits: Int, val fractionBits: Int, val iterations: Int, opList: Seq[CordicOp])
    extends Module {
  val io = IO(CordicPostprocessorIO(dataWidth = mantissaBits + fractionBits))

  val opListLength = opList.length

  io.out.cordic.getElements.foreach(_ := 0.S)
  io.out.dOut := 0.S

  for (i <- 0 until opListLength) {
    when(io.in.control.op === i.U) {
      val xPostProcess = opList(i).xPostProcess(io.in.cordic.x, io.in.control.xOpSpecific)
      val yPostProcess = opList(i).yPostProcess(io.in.cordic.y, io.in.control.yOpSpecific)
      val zPostProcess = opList(i).zPostProcess(io.in.cordic.z, io.in.control.zOpSpecific)
      io.out.cordic.x := xPostProcess
      io.out.cordic.y := yPostProcess
      io.out.cordic.z := zPostProcess
      io.out.dOut := {
        if (opList(i).resReg == CordicResultRegister.x) xPostProcess
        else if (opList(i).resReg == CordicResultRegister.y) yPostProcess
        else zPostProcess
      }
    }
  }

}

object CordicPostprocessor extends App {

  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array("--target", "systemverilog") ++ args },
    Seq(
      ChiselGeneratorAnnotation(() =>
        new CordicPostprocessor(
          4,
          12,
          14,
          Seq(
            CordicSine(4, 12, 14),
            CordicCosine(4, 12, 14)
          )
        )
      ),
    )
  )

}
