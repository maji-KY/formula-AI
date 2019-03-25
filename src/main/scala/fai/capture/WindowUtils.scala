package fai.capture

import java.awt.Rectangle

import com.sun.jna.platform.win32.WinDef

object WindowUtils {

  val TitleHeight = 31
  val ShadowWidth = 8

  def getWindowRectWithoutShadow(rect: WinDef.RECT): Rectangle = {
    new Rectangle(rect.left + ShadowWidth, rect.top + TitleHeight, rect.right - rect.left - (ShadowWidth * 2), rect.bottom - rect.top - (TitleHeight + ShadowWidth))
  }


}
