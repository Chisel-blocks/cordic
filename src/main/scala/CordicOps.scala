package accelerators

import chisel3._

trait CordicInfo {
  val mantissaBits: Int
  val fractionBits: Int
  val iterations: Int
}

object CordicRegister extends Enumeration {
  val rs1, rs2 = Value
}

object CordicResultRegister extends Enumeration {
  val x, y, z = Value
}

case class CordicInitAdd(val addA: CordicRegister.Value, val addB: Double)

abstract class CordicOp extends CordicInfo {
  // Fields that child classes have to implement
  val xInit: Either[Double, CordicInitAdd]
  val yInit: Either[Double, CordicInitAdd]
  val zInit: Either[Double, CordicInitAdd]
  val resReg: CordicResultRegister.Value
  val cordicMode: CordicMode.Type
  val rotationType: CordicRotationType.Type
}

abstract class CordicSine extends CordicOp {
  override val xInit        = Left(CordicMethods.calcK(iterations, CordicRotationType.CIRCULAR))
  override val yInit        = Left(0.0)
  override val zInit        = Right(CordicInitAdd(CordicRegister.rs1, 0))
  override val resReg       = CordicResultRegister.y
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}

abstract class CordicCosine extends CordicOp {
  override val xInit        = Left(CordicMethods.calcK(iterations, CordicRotationType.CIRCULAR))
  override val yInit        = Left(0.0)
  override val zInit        = Right(CordicInitAdd(CordicRegister.rs1, 0))
  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}
