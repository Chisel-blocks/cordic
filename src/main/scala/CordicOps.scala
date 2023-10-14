package accelerators

import chisel3._
import chisel3.util.{MuxCase, Cat}

abstract class CordicInit(val addA: CordicRegister.Type, val addB: Double)
class CordicInitAdd(addA: CordicRegister.Type, addB: Double) extends CordicInit(addA, addB)
class CordicInitSub(addA: CordicRegister.Type, addB: Double) extends CordicInit(addA, addB)

/** Abstract class CordicOp. It defines methods and values that child classes can/must implement.
  *
  * @param mantissaBits
  *   How many mantissa bits are used
  * @param fractionBits
  *   How many fractional bits are used
  * @param iterations
  *   How many iterations the CORDIC does
  */
abstract class CordicOp(mantissaBits: Int, fractionBits: Int, iterations: Int) {

  /** Preprocess X value. This operation is performed before feeding values to CordicCore. Return value consists of an
    * SInt value that holds the value to be fed to CordicCore, and an optional control signal that will be propagated
    * all the way to postprocessing.
    *
    * @param rs1
    *   Cordic register rs1
    * @param rs2
    *   Cordic register rs2
    * @param rs3
    *   Cordic register rs3
    * @return
    *   Tuple of (preprocessed X value, control signal)
    */
  def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (0.S, 0.U)
  }

  /** Preprocess Y value. This operation is performed before feeding values to CordicCore. Return value consists of an
    * SInt value that holds the value to be fed to CordicCore, and an optional control signal that will be propagated
    * all the way to postprocessing.
    *
    * @param rs1
    *   Cordic register rs1
    * @param rs2
    *   Cordic register rs2
    * @param rs3
    *   Cordic register rs3
    * @return
    *   Tuple of (preprocessed Y value, control signal)
    */
  def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (0.S, 0.U)
  }

  /** Preprocess Z value. This operation is performed before feeding values to CordicCore. Return value consists of an
    * SInt value that holds the value to be fed to CordicCore, and an optional control signal that will be propagated
    * all the way to postprocessing.
    *
    * @param rs1
    *   Cordic register rs1
    * @param rs2
    *   Cordic register rs2
    * @param rs3
    *   Cordic register rs3
    * @return
    *   Tuple of (preprocessed Z value, control signal)
    */
  def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (0.S, 0.U)
  }

  /** Postprocess X value. This operation is performed after CordicCore has finished.
    *
    * @param xReg
    *   X value from CordicCore
    * @param control
    *   Optional control signal that was defined in xPreProcess
    * @return
    *   Postprocessed X value
    */
  def xPostProcess(xReg: SInt, control: UInt): SInt = {
    xReg
  }

  /** Postprocess Y value. This operation is performed after CordicCore has finished.
    *
    * @param yReg
    *   Y value from CordicCore
    * @param control
    *   Optional control signal that was defined in yPreProcess
    * @return
    *   Postprocessed Y value
    */
  def yPostProcess(yReg: SInt, control: UInt): SInt = {
    yReg
  }

  /** Postprocess Z value. This operation is performed after CordicCore has finished.
    *
    * @param yReg
    *   Z value from CordicCore
    * @param control
    *   Optional control signal that was defined in zPreProcess
    * @return
    *   Postprocessed Z value
    */
  def zPostProcess(zReg: SInt, control: UInt): SInt = {
    zReg
  }

  /** Result register: in which CORDIC signal (x,y,z) the result is */
  val resReg: CordicResultRegister.Type

  /** Cordic mode used by the operation (ROTATION, VECTORING) */
  val cordicMode: CordicMode.Type

  /** Rotation type used by the operation (CIRCULAR, HYPERBOLIC) */
  val rotationType: CordicRotationType.Type

  /** An instance of CordicConstants */
  val consts = CordicConstants(mantissaBits, fractionBits, iterations)
}

case class CordicSine(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.K, 0.U)
  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (0.S, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    val largerThanPiOver2     = rs1 > consts.pPiOver2
    val smallerThanNegPiOver2 = rs1 < consts.nPiOver2
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
    val smallerThanNegPiOver2 = rs1 < consts.nPiOver2
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

case class CordicArctan(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits), 0.U)
  }

  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (rs1, 0.U)
  }

  override val resReg       = CordicResultRegister.z
  override val cordicMode   = CordicMode.VECTORING
  override val rotationType = CordicRotationType.CIRCULAR
}

case class CordicSinh(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.Kh, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (rs1, 0.U)
  }

  override val resReg       = CordicResultRegister.y
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.HYPERBOLIC
}

case class CordicCosh(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.Kh, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (rs1, 0.U)
  }

  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.HYPERBOLIC
}

case class CordicExponential(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.Kh, 0.U)
  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = (consts.Kh, 0.U)

  override def zPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (rs1, 0.U)
  }

  override val resReg       = CordicResultRegister.x
  override val cordicMode   = CordicMode.ROTATION
  override val rotationType = CordicRotationType.HYPERBOLIC
}

case class CordicArctanh(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits), 0.U)
  }

  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    (rs1, 0.U)
  }

  override val resReg       = CordicResultRegister.z
  override val cordicMode   = CordicMode.VECTORING
  override val rotationType = CordicRotationType.HYPERBOLIC
}

case class CordicLog(mantissaBits: Int, fractionBits: Int, iterations: Int)
    extends CordicOp(mantissaBits, fractionBits, iterations) {

  override def xPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    val retWire = rs1 + CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits)
    (retWire, 0.U)
  }

  override def yPreProcess(rs1: SInt, rs2: SInt, rs3: SInt): (SInt, UInt) = {
    val retWire = rs1 - CordicMethods.toFixedPoint(1.0, mantissaBits, fractionBits)
    (retWire, 0.U)
  }

  override def zPostProcess(zReg: SInt, control: UInt): SInt = {
    zReg << 1
  }

  override val resReg       = CordicResultRegister.z
  override val cordicMode   = CordicMode.VECTORING
  override val rotationType = CordicRotationType.HYPERBOLIC
}
