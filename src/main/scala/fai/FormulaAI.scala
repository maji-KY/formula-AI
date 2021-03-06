package fai

import java.awt.Robot
import java.awt.event.KeyEvent
import java.nio.file.Paths
import java.util.concurrent.{ConcurrentLinkedDeque, Executors}
import java.util.{Timer, TimerTask}

import scala.collection.JavaConverters._
import fai.capture.ScreenCaptor
import fai.control.{ControlState, KeyBoardOutPut}
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

object FormulaAI extends JFXApp with Logging {

  val executor = Executors.newFixedThreadPool(30)
  implicit val ec = ExecutionContext.fromExecutor(executor)
  val robot = new Robot()
  val keyboard = new KeyBoardOutPut(robot)
  var captor: Option[ScreenCaptor] = None

  val conf: Config = pureconfig
    .loadConfig[Config]
    .fold(fa => {
      fa.toList.foreach(x => log.error(x.toString))
      sys.error("config error")
    }, identity)

  val frameCount = 15
  val predictor = new Predictor(conf.modelPath)
  predictor.warmingUp()
  private[this] val framesData = new ConcurrentLinkedDeque[Array[Float]]()

  val writableImage = new WritableImage(224, 224)

  // data capture scheduler
  val captureIntervalMilliSec = 50 // 1 / 20 sec.

  val scheduler = new Timer()
  scheduler.schedule(new TimerTask {
    def run(): Unit = {
      // capture
      captor.foreach { x =>
        Future {
          val image = x.createScreenCapture()
          val resized = ImageUtils.resizeImage(image)
          framesData.addLast(Predictor.toByteArray(resized))
          Platform.runLater {
            imageView.setImage(SwingFXUtils.toFXImage(resized, null))
          }
        }
      }
    }
  }, 1000, captureIntervalMilliSec)

  // AI scheduler
  val predictIntervalMilliSec = 200
  val aiScheduler = new Timer()
  aiScheduler.schedule(new TimerTask {
    def run(): Unit = {
      val runStart = System.currentTimeMillis()
      if (framesData.size() >= frameCount + 1 && predictor.isWarmedUp) {
        val latestFrames = framesData.iterator().asScala.take(frameCount).toArray
        while (framesData.size() > frameCount) {
          framesData.removeFirst()
        }

        val result = predictor.predict(latestFrames)
        val cs = new ControlState(result)

        if (autoPilotToggle.selected.value) {
          keyboard.updateKeyBoardState(cs)
        } else {
          keyboard.releaseAll()
        }

        // show status
        Platform.runLater {
          controlText.text = s"state: ${result}, steering: ${cs.getSteering()}, throttle: ${cs.getThrottle()}, brake: ${cs.getBrake()}"

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
          infoText.text = s"executionTime: ${System.currentTimeMillis() - runStart}"
        }
      }
    }

  }, 1000, captureIntervalMilliSec)


  override def stopApp() = {
    scheduler.cancel()
    aiScheduler.cancel()
    executor.shutdown()
    predictor.close()
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

  val apReleaseButton = new Button(text = "AP Release") {
    onAction = (e: ActionEvent) => {
      autoPilotToggle.selected = false
    }
  }

  val apTimerButton = new Button(text = "Auto Pilot after 5 seconds") {
    onAction = (e: ActionEvent) => Future {
      Thread.sleep(5000)
      autoPilotToggle.fire()
    }
  }

  val autoPilotToggle = new ToggleButton("Auto Pilot")

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
    title = "Formula AI"
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
          apReleaseButton,
          apTimerButton,
          autoPilotToggle,
          imageView
        )
      }
    }
  }

}
