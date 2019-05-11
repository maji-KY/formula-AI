package fai

import java.awt.image.BufferedImage
import java.nio.file.Path
import java.nio.{FloatBuffer, LongBuffer}

import org.slf4s.Logging
import org.tensorflow.framework.{ConfigProto, GPUOptions}
import org.tensorflow.{SavedModelBundle, Tensor}

import scala.collection.JavaConverters._

class Predictor(modelPath: Path) extends AutoCloseable with Logging {

  @volatile
  private[this] var warmedUp = false

  private val config = ConfigProto.newBuilder()
    .setGpuOptions(
      GPUOptions.newBuilder()
                .setForceGpuCompatible(false)
        .setPerProcessGpuMemoryFraction(0.15)
    )
    //    .setLogDevicePlacement(true)
    .build()

  private val model = SavedModelBundle.loader(modelPath.toFile.getAbsolutePath)
    .withTags("serve")
    .withConfigProto(config.toByteArray)
    .load()

  private val session = model.session()

  def predict(frames: Array[Array[Float]]): Long = {
    log.trace(s"frames.length: ${frames.length}")
    val allImages = frames.foldLeft(Array.empty[Float])((a, b) => a ++ b )
    val floatBuf = FloatBuffer.wrap(allImages)
    val input = Tensor.create(Array[Long](1, frames.length, 224, 224, 3), floatBuf)
    val result = session.runner().feed("input", input).fetch("predicted_label", 0).run()
    val buf = LongBuffer.allocate(1)
    result.get(0).writeTo(buf)
    input.close()
    result.asScala.foreach(_.close())
    buf.array()(0)
  }

  def warmingUp(): Unit = {
    val dummy = Predictor.toByteArray(new BufferedImage(224, 224, BufferedImage.TYPE_3BYTE_BGR))
    predict(Array.fill(15)(dummy))
    warmedUp = true
    log.info("warmedUp!!!!!!!!!!!!!!")
  }

  def isWarmedUp: Boolean = warmedUp

  def close(): Unit = {
    session.close()
  }

}

object Predictor {

  def toByteArray(bi: BufferedImage): Array[Float] = {
    bi.getData().getPixels(0, 0, 224, 224, Array.ofDim[Float](224 * 224 * 3))
  }

}
