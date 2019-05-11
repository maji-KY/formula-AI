package fai

import java.awt.Robot
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong
import java.util.{Timer, TimerTask}

import fai.capture.ScreenCaptor
import fai.control.{ControlState, GamePadInput}
import javax.imageio.ImageIO
import org.slf4s.Logging
import scalafx.Includes._
import scalafx.application.JFXApp.PrimaryStage
import scalafx.application.{JFXApp, Platform}
import scalafx.collections.ObservableBuffer
import scalafx.embed.swing.SwingFXUtils
import scalafx.event.ActionEvent
import scalafx.scene.Scene
import scalafx.scene.chart.{BarChart, CategoryAxis, NumberAxis, XYChart}
import scalafx.scene.control.{Button, ToggleButton}
import scalafx.scene.image.{ImageView, WritableImage}
import scalafx.scene.layout.VBox
import scalafx.scene.text.Text

import scala.concurrent.{ExecutionContext, Future}

object LearningDataMaker extends JFXApp with Logging {

  val executor = Executors.newFixedThreadPool(30)
  implicit val ec = ExecutionContext.fromExecutor(executor)
  val robot = new Robot()
  var captor: Option[ScreenCaptor] = None

  val imagesCounter = new AtomicLong(0)

  val conf: Config = pureconfig
    .loadConfig[Config]
    .fold(fa => {
      fa.toList.foreach(x => log.error(x.toString))
      sys.error("config error")
    }, identity)
  log.info(conf.toString)

  // prepare controller
  val gamePadInput = new GamePadInput

  val writableImage = new WritableImage(224, 224)

  // data capture scheduler
  val captureIntervalMilliSec = 50 // 1 / 20 sec.
  val maxCaptureCount = 30 * (/* 1 min. */ 60 * 20)
  val scheduler = new Timer()
  scheduler.schedule(new TimerTask {
    def run(): Unit = {
      val stateLong = gamePadInput.getState()
      val cs = new ControlState(stateLong)

      val captureStart = System.currentTimeMillis()

      // capture
      captor.foreach { x =>
        Future {
          val image = x.createScreenCapture()
          val resized = ImageUtils.resizeImage(image)
          Platform.runLater {
            infoText.text = s"executionTime: ${System.currentTimeMillis() - captureStart}"
            imageView.setImage(SwingFXUtils.toFXImage(resized, null))
          }
          if (recordToggle.selected.value && imagesCounter.getAndIncrement() < maxCaptureCount) {
            ImageIO.write(resized, "jpg", conf.dataPath.resolve(s"${imagesCounter.get()}_${cs.getState}.jpg").toFile)
            Platform.runLater {
              countText.text = s"data count: ${imagesCounter.get()}"
            }
          }
        }
      }

      // show status
      Platform.runLater {
        controlText.text = s"state: ${cs.getState}, steering: ${cs.getSteering()}, throttle: ${cs.getThrottle()}, brake: ${cs.getBrake()}"

        val steeringPosition = if (cs.getSteering() == ControlState.SteeringStraight) 0 else if (cs.getSteering() == ControlState.SteeringLeft) -1 else 1
        val acceleration =  if (cs.getBrake()) -1 else if (cs.getThrottle()) 1 else 0
        steeringBar.data = ObservableBuffer(
          XYChart.Series[Number, String](
            "",
            ObservableBuffer(XYChart.Data[Number, String](steeringPosition, ""))
          )
        )
        accelerationBar.data = ObservableBuffer(
          XYChart.Series[String, Number](
            "",
            ObservableBuffer(XYChart.Data[String, Number]("", acceleration)),
          )
        )
      }

    }

  }, 1000, captureIntervalMilliSec)


  override def stopApp() = {
    scheduler.cancel()
    executor.shutdown()
  }

  val infoText = new Text("info")
  val countText = new Text("0")
  val capturePositionText = new Text("capture position")
  val controlText = new Text("control")

  val buildCaptorButton = new Button(text = "buildCaptor") {
    onAction = (e: ActionEvent) => Future {
      Thread.sleep(3000)
      val c = ScreenCaptor.getCurrentActiveWindowCaptor(robot)
      captor = Some(c)
      Platform.runLater {
        capturePositionText.text = s"${c.rectangle.toString}"
      }
    }
  }

  val captureButton = new Button(text = "Start recording after 5 seconds") {
    onAction = (e: ActionEvent) => Future {
      Thread.sleep(5000)
      recordToggle.fire()
    }
  }

  val recordToggle = new ToggleButton("Record")

  val imageView = new ImageView()

  def axis = new NumberAxis {
    lowerBound = -1
    upperBound = 1
    autoRanging = false
    tickUnit = 1
  }
  def category = new CategoryAxis()
  val steeringBar: BarChart[Number, String] = new BarChart(axis, category) {
    title = "Steering Control"
    prefHeight = 20
    data = ObservableBuffer(
      XYChart.Series[Number, String](
        "",
        ObservableBuffer(XYChart.Data[Number, String](0, ""))
      )
    )
  }
  val accelerationBar: BarChart[String, Number] = new BarChart(category, axis) {
    title = "Acceleration Control"
    prefHeight = 20
    data = ObservableBuffer(
      XYChart.Series[String, Number](
        "",
        ObservableBuffer(XYChart.Data[String, Number]("", 0)),
      )
    )
  }

 stage = new PrimaryStage {
    title = "LearningDataMaker"
    width = 1000
    height = 1000
    scene = new Scene {
      stylesheets.add(this.getClass.getClassLoader.getResource("style.css").toString)
      content = new VBox {
        children = Seq(
          infoText,
          countText,
          capturePositionText,
          steeringBar,
          controlText,
          accelerationBar,
          buildCaptorButton,
          captureButton,
          recordToggle,
          imageView
        )
      }
    }
  }

}
