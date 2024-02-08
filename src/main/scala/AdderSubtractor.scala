package accelerators

import chisel3._
import chisel3.stage.{ChiselStage}
import chisel3.stage.ChiselGeneratorAnnotation

case class AdderSubtractorIO(bits: Int) extends Bundle {
  val A = Input(SInt(bits.W))
  val B = Input(SInt(bits.W))
  val D = Input(Bool())
  val S = Output(SInt(bits.W))
}

/** Adder Subtractor combinatorial module. 
  * Calculates either addition or 
  * subtraction depending on input D.
  *
  * D = false -> Addition
  * 
  * D = true -> Subtraction
  *
  * @param bits
  *   How many bits inputs and outputs are
  */
class AdderSubtractor(bits: Int) extends Module {
  val io    = IO(AdderSubtractorIO(bits))
  val S_vec = Wire(Vec(bits, UInt(1.W)))
  val C_vec = Wire(Vec(bits + 1, UInt(1.W)))

  C_vec(0) := io.D

  for (i <- (0 until bits)) {
    val b = io.B(i) ^ io.D
    S_vec(i)     := b ^ io.A(i) ^ C_vec(i)
    C_vec(i + 1) := (b & io.A(i)) | (C_vec(i) & (b ^ io.A(i)))
  }

  io.S := S_vec.asTypeOf(SInt(bits.W))
}

object AdderSubtractor extends App {
  // These lines generate the Verilog output
  (new ChiselStage).execute(
    { Array("--target", "systemverilog") ++ args },
    Seq(ChiselGeneratorAnnotation(() => new AdderSubtractor(16)))
  )
}
