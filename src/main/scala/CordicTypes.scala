package accelerators

import chisel3._
import chisel3.util._

object TrigonometricOp extends ChiselEnum {
  val SINE        = Value(0.U)
  val COSINE      = Value(1.U)
  val ARCTAN      = Value(2.U)
  val SINH        = Value(3.U)
  val COSH        = Value(4.U)
  val ARCTANH     = Value(5.U)
  val EXPONENTIAL = Value(6.U)
  val LOG         = Value(7.U)
}

case class CordicBundle(dataWidth: Int) extends Bundle {
  val x = UInt(dataWidth.W)
  val y = UInt(dataWidth.W)
  val z = UInt(dataWidth.W)
}

case class CordicCoreControl() extends Bundle {
  val m     = UInt(1.W)
  val sigma = UInt(1.W)
}
