package accelerators

import chisel3._
import chisel3.util._
import scala.math.{atan, abs, floor, log, pow}

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

object CordicConstants {
  val hyperbolicRepeatIndices = Seq(4, 13, 40)
}

case class CordicBundle(dataWidth: Int) extends Bundle {
  val x = SInt(dataWidth.W)
  val y = SInt(dataWidth.W)
  val z = SInt(dataWidth.W)
}

case class CordicCoreControl() extends Bundle {

  /** circular/hyperbolic */
  val m = UInt(1.W)

  /** rotation/vectoring */
  val sigma = UInt(1.W)
}

object CordicMethods {

  def modf(value: Double): (Int, Double) = {
    val integer = value.toInt
    (integer, value - integer)
  }

  def toFixedPoint(value: Double, mantissaBits: Int, fractionBits: Int): SInt = {
    val (integerSigned, frac) = modf(value)
    val sign                  = value < 0
    val integer               = abs(integerSigned)
    val fracBits              = floor((1 << fractionBits) * abs(frac)).toInt
    var bits                  = (integer << fractionBits) | fracBits
    if (sign) {
      bits = bits * -1
    }
    bits.S
  }

}

case class CordicLut(mantissaBits: Int, fractionBits: Int, iterations: Int) {

  def atanh(x: Double): Double = {
    0.5 * log((1 + x) / (1 - x))
  }

  val atanVals = Seq.tabulate(iterations)(i =>
    CordicMethods.toFixedPoint(atan(pow(2, -i)), mantissaBits, fractionBits)
  )

  val atanhVals = Seq.tabulate(iterations)(i =>
    CordicMethods.toFixedPoint(atanh(pow(2, -i)), mantissaBits, fractionBits)
  )

}
