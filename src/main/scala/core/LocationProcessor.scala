package core
import models.*
import models.Settings.*

object LocationProcessor:
  def calculateNewLocation(playerData: PlayerData, tickData: TickData, speed: Double): (Double, Double) = {
    val currentLocX = playerData.locX
    val currentLocY = playerData.locY
    val xVector = tickData.xVector
    val yVector = tickData.yVector

    val isAtLeftBoundary = currentLocX < 0 && xVector < 0
    val isAtRightBoundary = currentLocX > worldWidth && xVector > 0
    val isAtTopBoundary = currentLocY < 0 && yVector > 0
    val isAtBottomBoundary = currentLocY > worldHeight && yVector < 0

    val newLocX = if isAtLeftBoundary || isAtRightBoundary then {
      currentLocX
    } else {
      currentLocX + speed * xVector
    }

    val newLocY = if isAtTopBoundary || isAtBottomBoundary then {
      currentLocY
    } else {
      currentLocY - speed * yVector
    }

    (newLocX, newLocY)
  }
