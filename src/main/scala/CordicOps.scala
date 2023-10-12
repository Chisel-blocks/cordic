package accelerators

import chisel3._

trait CordicInfo {
  val mantissaBits: Int
  val fractionBits: Int
  val iterations: Int
}

object CordicRegister extends ChiselEnum {
  val rs1, rs2 = Value
}

object CordicResultRegister extends ChiselEnum {
  val x, y, z = Value
}

abstract class CordicInit(val addA: CordicRegister.Type, val addB: Double)
class CordicInitAdd(addA: CordicRegister.Type, addB: Double) extends CordicInit(addA, addB)
class CordicInitSub(addA: CordicRegister.Type, addB: Double) extends CordicInit(addA, addB)

abstract class CordicOp extends CordicInfo {
  // Fields that child classes have to implement
  val xInit: Either[Double, CordicInit]
  val yInit: Either[Double, CordicInit]
  val zInit: Either[Double, CordicInit]
  val resReg: CordicResultRegister.Type
  val cordicMode: CordicMode.Type
  val rotationType: CordicRotationType.Type
}

abstract class CordicSine extends CordicOp {
  override val xInit        = Left(CordicMethods.calcK(iterations, CordicRotationType.CIRCULAR))
  override val yInit        = Left(0.0)
  override val zInit        = Right(new CordicInitAdd(CordicRegister.rs1, 0))
  override val resReg       = CordicResultRegister.y
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}

abstract class CordicCosine extends CordicOp {
  override val xInit        = Left(CordicMethods.calcK(iterations, CordicRotationType.CIRCULAR))
  override val yInit        = Left(0.0)
  override val zInit        = Right(new CordicInitAdd(CordicRegister.rs1, 0))
  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}

abstract class CordicLog extends CordicOp {
  override val xInit        = Right(new CordicInitAdd(CordicRegister.rs1, 1.0))
  override val yInit        = Right(new CordicInitSub(CordicRegister.rs1, 1.0))
  override val zInit        = Left(0.0)
  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}
