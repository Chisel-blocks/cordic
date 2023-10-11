package accelerators

import chisel3._
import chisel3.util._
import scala.math.{atan, abs, floor, log, pow, sqrt}

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

object CordicMode extends ChiselEnum {
  val ROTATION  = Value(0.U)
  val VECTORING = Value(1.U)
}

object CordicRotationType extends ChiselEnum {
  val CIRCULAR   = Value(0.U)
  val HYPERBOLIC = Value(1.U)
}

case class CordicCoreControl() extends Bundle {

  /** circular/hyperbolic */
  val rotType = CordicRotationType()

  /** rotation/vectoring */
  val mode = CordicMode()
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

  def calcK(iterations: Int, rotationType: CordicRotationType.Type): Double = {
    val iInit = {if (rotationType == CordicRotationType.CIRCULAR) 0 else 1}   
    var k = 1.0
    for (i <- iInit until iterations) {
      val sqrtee = {
        if (rotationType == CordicRotationType.CIRCULAR) 1 + pow(2, -2 * i)
        else 1 - pow(2, -2 * i)
      }
      k *= sqrt(sqrtee)
      if (CordicConstants.hyperbolicRepeatIndices.contains(i))
        k *= sqrt(sqrtee)
    }
    0.0
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
