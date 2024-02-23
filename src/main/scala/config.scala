// SPDX-License-Identifier: Apache-2.0

package cordic.config

import pureconfig._
import pureconfig.generic.auto._
import pureconfig.generic.semiauto._
import pureconfig.generic.ProductHint
import pureconfig.module.yaml._
import chisel3._

case class CordicConfig(
  mantissaBits: Int,
  fractionBits: Int,
  iterations: Int,
  preprocessorClass: String,
  postprocessorClass: String
)

object CordicConfig {
  def loadFromFile(filename: String): CordicConfig = {
    val source = YamlConfigSource.file(filename)
    val config = source.loadOrThrow[CordicConfig]
    config
  }
}