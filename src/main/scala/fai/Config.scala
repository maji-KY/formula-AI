package fai

import java.nio.file.Paths

case class Config(
                 dataDir: String,
                 modelDir: String
                 ) {
  val dataPath = Paths.get(dataDir)
  val modelPath = Paths.get(modelDir)
}