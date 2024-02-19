package cordic

import chisel3._
import chisel3.util._
import chisel3.experimental._
import scala.math.{atan, abs, floor, log, pow, sqrt}

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

  /** Custom control bits to transmit info between pre- and postprocessor */
  val custom = UInt(32.W)
}

object CordicMethods {

  def modf(value: Double): (Int, Double) = {
    val integer = value.toInt
    (integer, value - integer)
  }

  def toFixedPoint(value: Double, mantissaBits: Int, fractionBits: Int, repr: String): SInt = {
    if (repr == "fixed-point") {
      val (integerSigned, frac) = modf(value)
      val sign                  = value < 0
      val integer               = abs(integerSigned)
      val fracBits              = floor((1 << fractionBits) * abs(frac)).toInt
      var bits                  = (integer << fractionBits) | fracBits
      if (sign) {
        bits = bits * -1
      }
      bits.S
    } else if (repr == "pi") {
      ((value * pow(2, mantissaBits + fractionBits - 1)) / math.Pi).toLong.S
    } else {
      throw new RuntimeException(s"Incorrect repr type: $repr")
    }
  }

  def calcK(iterations: Int, rotationType: CordicRotationType.Type): Double = {
    val iInit = { if (rotationType == CordicRotationType.CIRCULAR) 0 else 1 }
    var k     = 1.0
    for (i <- iInit until iterations) {
      val sqrtee = {
        if (rotationType == CordicRotationType.CIRCULAR) 1 + pow(2, -2 * i)
        else 1 - pow(2, -2 * i)
      }
      k *= sqrt(sqrtee)
      if (CordicConstants.hyperbolicRepeatIndices.contains(i))
        k *= sqrt(sqrtee)
    }
    k
  }

}

case class CordicConstants(mantissaBits: Int, fractionBits: Int, iterations: Int, repr: String) {

  val K =
    CordicMethods.toFixedPoint(
      1.0 / CordicMethods.calcK(iterations, CordicRotationType.CIRCULAR),
      mantissaBits,
      fractionBits,
      repr
    )

  val Kh = CordicMethods.toFixedPoint(
    1.0 / CordicMethods.calcK(iterations, CordicRotationType.HYPERBOLIC),
    mantissaBits,
    fractionBits,
    repr
  )

  val pPi      = CordicMethods.toFixedPoint(math.Pi, mantissaBits, fractionBits, repr)
  val pPiOver2 = CordicMethods.toFixedPoint(math.Pi / 2, mantissaBits, fractionBits, repr)
  val nPiOver2 = CordicMethods.toFixedPoint(-math.Pi / 2, mantissaBits, fractionBits, repr)

}

case class CordicLut(mantissaBits: Int, fractionBits: Int, iterations: Int, repr: String) {

  def atanh(x: Double): Double = {
    0.5 * log((1 + x) / (1 - x))
  }

  val atanVals = Seq.tabulate(iterations)(i =>
    CordicMethods.toFixedPoint(atan(pow(2, -i)), mantissaBits, fractionBits, repr)
  )

  val atanhVals = Seq.tabulate(iterations)(i =>
    CordicMethods.toFixedPoint(atanh(pow(2, -i)), mantissaBits, fractionBits, repr)
  )

}
