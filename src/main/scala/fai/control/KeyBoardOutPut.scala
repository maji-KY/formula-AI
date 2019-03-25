package fai.control

import java.awt.Robot
import java.awt.event.KeyEvent

import org.slf4s.Logging

class KeyBoardOutPut(robot: Robot) extends Controller with Logging  {

  def updateKeyBoardState(state: ControlState): Unit = {
    val steering = state.getSteering()
    if (steering == ControlState.SteeringLeft) {
      robot.keyPress(KeyEvent.VK_LEFT)
      robot.keyRelease(KeyEvent.VK_RIGHT)
    } else if (steering == ControlState.SteeringRight) {
      robot.keyRelease(KeyEvent.VK_LEFT)
      robot.keyPress(KeyEvent.VK_RIGHT)
    } else {
      robot.keyRelease(KeyEvent.VK_LEFT)
      robot.keyRelease(KeyEvent.VK_RIGHT)
    }
    if (state.getThrottle()) {
      robot.keyPress(KeyEvent.VK_SPACE)
    } else {
      robot.keyRelease(KeyEvent.VK_SPACE)
    }
    if (state.getBrake()) {
      robot.keyPress(KeyEvent.VK_CONTROL)
    } else {
      robot.keyRelease(KeyEvent.VK_CONTROL)
    }
  }

  def releaseAll(): Unit = {
    robot.keyRelease(KeyEvent.VK_LEFT)
    robot.keyRelease(KeyEvent.VK_RIGHT)
    robot.keyRelease(KeyEvent.VK_SPACE)
    robot.keyRelease(KeyEvent.VK_CONTROL)
  }

}
