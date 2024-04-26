// SPDX-License-Identifier: Apache-2.0

package cordic.config

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.generic.semiauto._
import pureconfig.generic.ProductHint
import pureconfig.module.yaml._
import chisel3._

case class UpConvertConfig(
  usePhaseAccum: Boolean,
  phaseAccumWidth: Int
)

case class CordicConfig(
  mantissaBits: Int,
  fractionBits: Int,
  iterations: Int,
  preprocessorClass: String = "Basic",
  postprocessorClass: String = "Basic",
  numberRepr: String = "fixed-point",
  enableCircular: Boolean = false,
  enableHyperbolic: Boolean = false,
  enableRotational: Boolean = false,
  enableVectoring: Boolean = false,
  usedInputs: List[Int] = List(0, 1, 2),
  upConvertConfig: Option[UpConvertConfig]
)

object CordicConfig {
  def loadFromFile(filename: String): CordicConfig = {
    val source = YamlConfigSource.file(filename)
    val config = source.loadOrThrow[CordicConfig]
    config
  }
}