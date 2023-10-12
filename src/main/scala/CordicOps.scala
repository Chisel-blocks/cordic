package accelerators

import chisel3._
import chisel3.util.{MuxCase, Cat}

abstract class CordicInit(val addA: CordicRegister.Type, val addB: Double)
class CordicInitAdd(addA: CordicRegister.Type, addB: Double) extends CordicInit(addA, addB)
class CordicInitSub(addA: CordicRegister.Type, addB: Double) extends CordicInit(addA, addB)

abstract class CordicOp(mantissaBits: Int, fractionBits: Int, iterations: Int) {

  def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (0.S, 0.U)
  }

  def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (0.S, 0.U)
  }

  def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (0.S, 0.U)
  }

  def xPostProcess(xReg: SInt, control: UInt): SInt = {
    xReg
  }

  def yPostProcess(yReg: SInt, control: UInt): SInt = {
    yReg
  }

  def zPostProcess(zReg: SInt, control: UInt): SInt = {
    zReg
  }

  val resReg: CordicResultRegister.Type
  val cordicMode: CordicMode.Type
  val rotationType: CordicRotationType.Type
  val consts = CordicConstants(mantissaBits, fractionBits, iterations)
}

case class CordicSine(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.K, 0.U)
  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (0.S, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    val largerThanPiOver2     = rs1 > consts.pPiOver2
    val smallerThanNegPiOver2 = rs2 < consts.nPiOver2
    val retWire =
      MuxCase(rs1, Seq(largerThanPiOver2 -> { rs1 - consts.pPi }, smallerThanNegPiOver2 -> { rs1 + consts.pPi }))
    (retWire, Cat(largerThanPiOver2, smallerThanNegPiOver2))
  }

  override def yPostProcess(yReg: SInt, control: UInt): SInt = {
    Mux(control(0) || control(1), ~yReg + 1.S, yReg)
  }

  override val resReg       = CordicResultRegister.y
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}

case class CordicCosine(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.K, 0.U)
  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (0.S, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    val largerThanPiOver2     = rs1 > consts.pPiOver2
    val smallerThanNegPiOver2 = rs2 < consts.nPiOver2
    val retWire =
      MuxCase(rs1, Seq(largerThanPiOver2 -> { rs1 - consts.pPi }, smallerThanNegPiOver2 -> { rs1 + consts.pPi }))
    (retWire, Cat(largerThanPiOver2, smallerThanNegPiOver2))
  }

  override def xPostProcess(yReg: SInt, control: UInt): SInt = {
    Mux(control(0) || control(1), ~yReg + 1.S, yReg)
  }

  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}

case class CordicLog(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.K, 0.U)
  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (0.S, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    val largerThanPiOver2     = rs1 > consts.pPiOver2
    val smallerThanNegPiOver2 = rs2 < consts.nPiOver2
    val retWire =
      MuxCase(rs1, Seq(largerThanPiOver2 -> { rs1 - consts.pPi }, smallerThanNegPiOver2 -> { rs1 + consts.pPi }))
    (retWire, Cat(largerThanPiOver2, smallerThanNegPiOver2))
  }

  override def yPostProcess(yReg: SInt, control: UInt): SInt = {
    Mux(control(0) || control(1), ~yReg + 1.S, yReg)
  }

  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.CIRCULAR
}

/*  case class CordicCosine(iterations: Int) extends CordicOp(iterations) { override val xInit =
 * Left(CordicMethods.calcK(iterations, CordicRotationType.CIRCULAR)) override val yInit = Left(0.0) override val zInit
 * = Right(new CordicInitAdd(CordicRegister.rs1, 0)) override val resReg = CordicResultRegister.x override val
 * cordicMode = CordicMode.ROTATION override val rotationType = CordicRotationType.CIRCULAR }
 *
 * case class CordicArctan(iterations: Int) extends CordicOp(iterations) { override val xInit = Left(1.0) override val
 * yInit = Right(new CordicInitAdd(CordicRegister.rs1, 0.0)) override val zInit = Left(0.0) override val resReg =
 * CordicResultRegister.z override val cordicMode = CordicMode.VECTORING override val rotationType =
 * CordicRotationType.CIRCULAR }
 *
 * case class CordicArctanh(iterations: Int) extends CordicOp(iterations) { override val xInit = Left(1.0) override val
 * yInit = Right(new CordicInitAdd(CordicRegister.rs1, 0.0)) override val zInit = Left(0.0) override val resReg =
 * CordicResultRegister.z override val cordicMode = CordicMode.VECTORING override val rotationType =
 * CordicRotationType.HYPERBOLIC }
 *
 * case class CordicSinh(iterations: Int) extends CordicOp(iterations) { override val xInit =
 * Left(CordicMethods.calcK(iterations, CordicRotationType.HYPERBOLIC)) override val yInit = Left(0.0) override val
 * zInit = Right(new CordicInitAdd(CordicRegister.rs1, 0)) override val resReg = CordicResultRegister.y override val
 * cordicMode = CordicMode.ROTATION override val rotationType = CordicRotationType.HYPERBOLIC }
 *
 * case class CordicCosh(iterations: Int) extends CordicOp(iterations) { override val xInit =
 * Left(CordicMethods.calcK(iterations, CordicRotationType.HYPERBOLIC)) override val yInit = Left(0.0) override val
 * zInit = Right(new CordicInitAdd(CordicRegister.rs1, 0)) override val resReg = CordicResultRegister.x override val
 * cordicMode = CordicMode.ROTATION override val rotationType = CordicRotationType.HYPERBOLIC }
 *
 * case class CordicExponential(iterations: Int) extends CordicOp(iterations) { override val xInit =
 * Left(CordicMethods.calcK(iterations, CordicRotationType.HYPERBOLIC)) override val yInit =
 * Left(CordicMethods.calcK(iterations, CordicRotationType.HYPERBOLIC)) override val zInit = Right(new
 * CordicInitAdd(CordicRegister.rs1, 0)) override val resReg = CordicResultRegister.x override val cordicMode =
 * CordicMode.ROTATION override val rotationType = CordicRotationType.HYPERBOLIC }
 *
 * case class CordicLog(iterations: Int) extends CordicOp(iterations) { override val xInit = Right(new
 * CordicInitAdd(CordicRegister.rs1, 1.0)) override val yInit = Right(new CordicInitSub(CordicRegister.rs1, 1.0))
 * override val zInit = Left(0.0) override val resReg = CordicResultRegister.z override val cordicMode =
 * CordicMode.ROTATION override val rotationType = CordicRotationType.CIRCULAR } */
