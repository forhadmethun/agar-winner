package core

import core.LocationProcessor.calculateNewLocation
import models.*
import munit.FunSuite

class LocationProcessorTest extends FunSuite {
  val playerConfig: PlayerConfig = PlayerConfig(speed = 0, zoom = 0.0)
  val playerData: PlayerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
  val player: Player = Player(playerConfig, playerData)

  test("calculateNewLocation should calculate new location correctly") {
    val playerData = PlayerData(uid = "player1", sid = "sid1", playerName = "Player 1", locX = 10, locY = 10, color = "red", radius = 5)
    val tickData = TickData(uid = "player1", xVector = 1, yVector = -1)
    val speed = 0.1

    val (newLocX, newLocY) = calculateNewLocation(playerData, tickData, speed)

    assertEquals(newLocX, 10.1)
    assertEquals(newLocY, 10.1)
  }
}
