// SPDX-License-Identifier: Apache-2.0

// Chisel module CordicPreprocessor
// Inititally written by Aleksi Korsman (aleksi.korsman@aalto.fi), 2023-10-11
package accelerators

import chisel3._
import chisel3.util._

case class CordicPreprocessorIO(dataWidth: Int, nOps: Int) extends Bundle {

  val in = new Bundle {
    val rs1 = Input(SInt(dataWidth.W))
    val rs2 = Input(SInt(dataWidth.W))
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
    extends Module with CordicInfo {
  val io = CordicPreprocessorIO(dataWidth = mantissaBits + fractionBits, opList.length)

  val opListLength = opList.length

  val modeVec = VecInit.tabulate(opListLength)(i => opList(i).cordicMode)
  val typeVec = VecInit.tabulate(opListLength)(i => opList(i).rotationType)

  val addeeVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Right(addValues) => {
        addValues match {
          case addedValue: CordicInitAdd => CordicMethods.toFixedPoint(addedValue.addB, mantissaBits, fractionBits)
          case _                         => 0.S
        }
      }
      case _ => 0.S
    }
  })

  val subbeeVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Right(subValues) => {
        subValues match {
          case subbedValue: CordicInitSub => CordicMethods.toFixedPoint(subbedValue.addB, mantissaBits, fractionBits)
          case _                          => 0.S
        }
      }
      case _ => 0.S
    }
  })

  val regAddeeList = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Right(addValues) => addValues.addA
      case _                => CordicRegister.rs1
    }
  })

  val cordicReg = Wire(chiselTypeOf(io.in.rs1))

  when(regAddeeList(io.in.op) === CordicRegister.rs1) {
    cordicReg := io.in.rs1
  }.otherwise {
    cordicReg := io.in.rs2
  }

  val addResult = cordicReg + addeeVec(io.in.op)
  val subResult = cordicReg - subbeeVec(io.in.op)

  val xSelectVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Left(_) => CordicSignalSelect.CONSTANT
      case Right(cordicInit) => {
        cordicInit match {
          case _: CordicInitAdd => CordicSignalSelect.ADD
          case _: CordicInitSub => CordicSignalSelect.SUB
        }
      }
    }
  })

  val ySelectVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Left(_) => CordicSignalSelect.CONSTANT
      case Right(cordicInit) => {
        cordicInit match {
          case _: CordicInitAdd => CordicSignalSelect.ADD
          case _: CordicInitSub => CordicSignalSelect.SUB
        }
      }
    }
  })

  val zSelectVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Left(_) => CordicSignalSelect.CONSTANT
      case Right(cordicInit) => {
        cordicInit match {
          case _: CordicInitAdd => CordicSignalSelect.ADD
          case _: CordicInitSub => CordicSignalSelect.SUB
        }
      }
    }
  })

  val xConstantVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Left(constant) => CordicMethods.toFixedPoint(constant, mantissaBits, fractionBits)
      case Right(_)       => 0.S
    }
  })

  val yConstantVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Left(constant) => CordicMethods.toFixedPoint(constant, mantissaBits, fractionBits)
      case Right(_)       => 0.S
    }
  })

  val zConstantVec = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Left(constant) => CordicMethods.toFixedPoint(constant, mantissaBits, fractionBits)
      case Right(_)       => 0.S
    }
  })

  io.out.cordic.x := MuxCase(
    xConstantVec(io.in.op),
    Seq(
      (xSelectVec(io.in.op) === CordicSignalSelect.ADD)      -> addResult,
      (xSelectVec(io.in.op) === CordicSignalSelect.SUB)      -> subResult,
      (xSelectVec(io.in.op) === CordicSignalSelect.CONSTANT) -> xConstantVec(io.in.op)
    )
  )

  io.out.cordic.y := MuxCase(
    yConstantVec(io.in.op),
    Seq(
      (ySelectVec(io.in.op) === CordicSignalSelect.ADD)      -> addResult,
      (ySelectVec(io.in.op) === CordicSignalSelect.SUB)      -> subResult,
      (ySelectVec(io.in.op) === CordicSignalSelect.CONSTANT) -> yConstantVec(io.in.op)
    )
  )

  io.out.cordic.z := MuxCase(
    zConstantVec(io.in.op),
    Seq(
      (ySelectVec(io.in.op) === CordicSignalSelect.ADD)      -> addResult,
      (ySelectVec(io.in.op) === CordicSignalSelect.SUB)      -> subResult,
      (ySelectVec(io.in.op) === CordicSignalSelect.CONSTANT) -> zConstantVec(io.in.op)
    )
  )

  io.out.control.mode    := modeVec(io.in.op)
  io.out.control.rotType := typeVec(io.in.op)

}
