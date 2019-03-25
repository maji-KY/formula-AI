package fai.control

import net.java.games.input.Component.Identifier
import net.java.games.input.Controller.Type
import net.java.games.input.ControllerEnvironment
import org.slf4s.Logging


class GamePadInput  extends Controller with Logging  {

  private def getGamepad() = {
    val controllers = ControllerEnvironment.getDefaultEnvironment().getControllers()
    val controller = controllers.find(c => c.getType == Type.GAMEPAD).getOrElse(throw new Exception("GAMEPAD not found"))
    log.info(controller.toString)
    controller
  }
  private val controller = getGamepad()

  def getState(): Long = {
    if (controller.poll()) {
      val povValue = controller.getComponent(Identifier.Axis.POV).getPollData
      val steering: Int = if (povValue > 0.75 || povValue == 0.125) {
        ControlState.SteeringLeft
      } else if (povValue > 0.25 && povValue < 0.75) {
        ControlState.SteeringRight
      } else {
        ControlState.SteeringStraight
      }
      val throttle: Boolean = controller.getComponent(Identifier.Button._0).getPollData == 1
      val brake: Boolean = controller.getComponent(Identifier.Button._2).getPollData == 1
      val cs = new ControlState()
      cs.setSteering(steering)
      cs.setThrottle(throttle)
      cs.setBrake(brake)
      cs.getState
    } else sys.error("controller.poll failed")
  }

}
