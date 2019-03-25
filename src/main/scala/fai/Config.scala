package fai

import java.nio.file.Paths

case class Config(
                 dataDir: String
                 ) {
  val dataPath = Paths.get(dataDir)
}