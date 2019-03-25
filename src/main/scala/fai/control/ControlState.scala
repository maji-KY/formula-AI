package fai.control

import scala.collection.mutable.BitSet

class ControlState(private[this] val bs: BitSet) {

  def this(state: Long) = this(BitSet.fromBitMask(Array(state)))

  def this() = this(BitSet.empty)

  import ControlState._

  def setThrottle(on: Boolean): Unit = if (on) {
    bs += 0
  }

  def setBrake(on: Boolean): Unit = if (on) {
    bs += 1
  }

  def setSteering(pos: Int): Unit = if (pos == SteeringLeft) {
    bs += 2
  } else if (pos == SteeringRight) {
    bs += 3
  }

  /**
    * @return state bit value(0 to 11)
    */
  def getState: Long = bs.toBitMask.head

  final def getThrottle(): Boolean = bs(0)

  final def getBrake(): Boolean = bs(1)

  final def getSteering(): Int = if (bs(2)) {
    SteeringLeft
  } else if (bs(3)) {
    SteeringRight
  } else {
    SteeringStraight
  }

}


object ControlState {
  val SteeringStraight = 0
  val SteeringLeft = 1
  val SteeringRight = 2
}