// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicPreprocessor
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-11
package accelerators

import chisel3._
import chisel3.util.{MuxCase, log2Ceil}
import circt.stage.{ChiselStage, FirtoolOption}
import chisel3.stage.ChiselGeneratorAnnotation

case class CordicPreprocessorIO(dataWidth: Int, nOps: Int) extends Bundle {

  val in = new Bundle {
    val rs1 = Input(SInt(dataWidth.W))
    val rs2 = Input(SInt(dataWidth.W))
    val rs3 = Input(SInt(dataWidth.W))
    val op  = Input(UInt(log2Ceil(nOps).W))
  }

  val out = new Bundle {
    val cordic  = Output(CordicBundle(dataWidth))
    val control = Output(CordicCoreControl())
  }

}

object CordicSignalSelect extends ChiselEnum {
  val CONSTANT, ADD, SUB = Value
}

class CordicPreprocessor(val mantissaBits: Int, val fractionBits: Int, val iterations: Int, opList: Seq[CordicOp])
    extends Module {
  val io = IO(CordicPreprocessorIO(dataWidth = mantissaBits + fractionBits, opList.length))

  val opListLength = opList.length

  val modeVec = VecInit.tabulate(opListLength)(i => opList(i).cordicMode)
  val typeVec = VecInit.tabulate(opListLength)(i => opList(i).rotationType)

  io.out.cordic.x            := 0.S
  io.out.cordic.y            := 0.S
  io.out.cordic.z            := 0.S
  io.out.control.xOpSpecific := 0.U
  io.out.control.yOpSpecific := 0.U
  io.out.control.zOpSpecific := 0.U

  for (i <- 0 until opListLength) {
    when(io.in.op === i.U) {
      val xPreProcess = opList(i).xPreProcess(io.in.rs1, io.in.rs2, io.in.rs3)
      val yPreProcess = opList(i).yPreProcess(io.in.rs1, io.in.rs2, io.in.rs3)
      val zPreProcess = opList(i).zPreProcess(io.in.rs1, io.in.rs2, io.in.rs3)
      io.out.cordic.x            := xPreProcess._1
      io.out.cordic.y            := yPreProcess._1
      io.out.cordic.z            := zPreProcess._1
      io.out.control.xOpSpecific := xPreProcess._2
      io.out.control.yOpSpecific := yPreProcess._2
      io.out.control.zOpSpecific := zPreProcess._2
    }
  }

  io.out.control.mode    := modeVec(io.in.op)
  io.out.control.rotType := typeVec(io.in.op)

}

object CordicPreprocessor extends App {

  // These lines generate the Verilog output
  (new circt.stage.ChiselStage).execute(
    { Array("--target", "systemverilog") ++ args },
    Seq(
      ChiselGeneratorAnnotation(() =>
        new CordicPreprocessor(
          4,
          12,
          14,
          Seq(
            CordicSine(4, 12, 14)
          )
        )
      ),
      FirtoolOption("--disable-all-randomization")
    )
  )

}
