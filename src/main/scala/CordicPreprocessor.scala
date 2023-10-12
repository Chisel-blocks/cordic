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

  val xAddeeVec = VecInit.tabulate(opListLength)(i => {
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

  val yAddeeVec = VecInit.tabulate(opListLength)(i => {
    opList(i).yInit match {
      case Right(addValues) => {
        addValues match {
          case addedValue: CordicInitAdd => CordicMethods.toFixedPoint(addedValue.addB, mantissaBits, fractionBits)
          case _                         => 0.S
        }
      }
      case _ => 0.S
    }
  })

  val zAddeeVec = VecInit.tabulate(opListLength)(i => {
    opList(i).zInit match {
      case Right(addValues) => {
        addValues match {
          case addedValue: CordicInitAdd => CordicMethods.toFixedPoint(addedValue.addB, mantissaBits, fractionBits)
          case _                         => 0.S
        }
      }
      case _ => 0.S
    }
  })

  val xSubbeeVec = VecInit.tabulate(opListLength)(i => {
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

  val ySubbeeVec = VecInit.tabulate(opListLength)(i => {
    opList(i).yInit match {
      case Right(subValues) => {
        subValues match {
          case subbedValue: CordicInitSub => CordicMethods.toFixedPoint(subbedValue.addB, mantissaBits, fractionBits)
          case _                          => 0.S
        }
      }
      case _ => 0.S
    }
  })

  val zSubbeeVec = VecInit.tabulate(opListLength)(i => {
    opList(i).zInit match {
      case Right(subValues) => {
        subValues match {
          case subbedValue: CordicInitSub => CordicMethods.toFixedPoint(subbedValue.addB, mantissaBits, fractionBits)
          case _                          => 0.S
        }
      }
      case _ => 0.S
    }
  })

  val xRegAddeeList = VecInit.tabulate(opListLength)(i => {
    opList(i).xInit match {
      case Right(addValues) => addValues.addA
      case _                => CordicRegister.rs1
    }
  })

  val yRegAddeeList = VecInit.tabulate(opListLength)(i => {
    opList(i).yInit match {
      case Right(addValues) => addValues.addA
      case _                => CordicRegister.rs1
    }
  })

  val zRegAddeeList = VecInit.tabulate(opListLength)(i => {
    opList(i).zInit match {
      case Right(addValues) => addValues.addA
      case _                => CordicRegister.rs1
    }
  })

  val xCordicReg = Wire(SInt())
  val yCordicReg = Wire(SInt())
  val zCordicReg = Wire(SInt())

  when(xRegAddeeList(io.in.op) === CordicRegister.rs1) {
    xCordicReg := io.in.rs1
  }.otherwise {
    xCordicReg := io.in.rs2
  }

  when(yRegAddeeList(io.in.op) === CordicRegister.rs1) {
    yCordicReg := io.in.rs1
  }.otherwise {
    yCordicReg := io.in.rs2
  }

  when(zRegAddeeList(io.in.op) === CordicRegister.rs1) {
    zCordicReg := io.in.rs1
  }.otherwise {
    zCordicReg := io.in.rs2
  }

  val xAddResult = xCordicReg + xAddeeVec(io.in.op)
  val xSubResult = xCordicReg - xSubbeeVec(io.in.op)
  val yAddResult = yCordicReg + yAddeeVec(io.in.op)
  val ySubResult = yCordicReg - ySubbeeVec(io.in.op)
  val zAddResult = zCordicReg + zAddeeVec(io.in.op)
  val zSubResult = zCordicReg - zSubbeeVec(io.in.op)

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
    opList(i).yInit match {
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
    opList(i).zInit match {
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
    opList(i).yInit match {
      case Left(constant) => CordicMethods.toFixedPoint(constant, mantissaBits, fractionBits)
      case Right(_)       => 0.S
    }
  })

  val zConstantVec = VecInit.tabulate(opListLength)(i => {
    opList(i).zInit match {
      case Left(constant) => CordicMethods.toFixedPoint(constant, mantissaBits, fractionBits)
      case Right(_)       => 0.S
    }
  })

  io.out.cordic.x := MuxCase(
    xConstantVec(io.in.op),
    Seq(
      (xSelectVec(io.in.op) === CordicSignalSelect.ADD)      -> xAddResult,
      (xSelectVec(io.in.op) === CordicSignalSelect.SUB)      -> xSubResult,
      (xSelectVec(io.in.op) === CordicSignalSelect.CONSTANT) -> xConstantVec(io.in.op)
    )
  )

  io.out.cordic.y := MuxCase(
    yConstantVec(io.in.op),
    Seq(
      (ySelectVec(io.in.op) === CordicSignalSelect.ADD)      -> yAddResult,
      (ySelectVec(io.in.op) === CordicSignalSelect.SUB)      -> ySubResult,
      (ySelectVec(io.in.op) === CordicSignalSelect.CONSTANT) -> yConstantVec(io.in.op)
    )
  )

  io.out.cordic.z := MuxCase(
    zConstantVec(io.in.op),
    Seq(
      (zSelectVec(io.in.op) === CordicSignalSelect.ADD)      -> zAddResult,
      (zSelectVec(io.in.op) === CordicSignalSelect.SUB)      -> zSubResult,
      (zSelectVec(io.in.op) === CordicSignalSelect.CONSTANT) -> zConstantVec(io.in.op)
    )
  )

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
            CordicSine(14),
            CordicCosine(14),
            CordicExponential(14),
            CordicSinh(14),
            CordicCosh(14),
            CordicExponential(14),
            CordicLog(14)
          )
        )
      ),
      FirtoolOption("--disable-all-randomization")
    )
  )

}
