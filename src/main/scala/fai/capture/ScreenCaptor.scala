package fai.capture

import java.awt.image.BufferedImage
import java.awt.{Rectangle, Robot}

import com.sun.jna.platform.win32.{MyGDI32Util, User32, WinDef}
import org.slf4s.Logging

class ScreenCaptor private (windowHandle: WinDef.HWND, val rectangle: Rectangle) extends Logging {

  def createScreenCapture(): BufferedImage = MyGDI32Util.getScreenshot(windowHandle, rectangle)

}

object ScreenCaptor {

  def getCurrentActiveWindowCaptor(robot: Robot): ScreenCaptor = {
    val target = User32.INSTANCE.GetForegroundWindow()
    val rect = new WinDef.RECT
    User32.INSTANCE.GetWindowRect(target, rect)
    new ScreenCaptor(target, WindowUtils.getWindowRectWithoutShadow(rect))
  }

}