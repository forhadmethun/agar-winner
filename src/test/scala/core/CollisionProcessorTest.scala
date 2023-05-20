package core

import core.CollisionProcessor.{calculateNewLocation, isAABBTestPassing, isOrbCollidingWithPlayer, isPythagorasTestPassing}
import models.{OrbData, PlayerData, TickData}
import munit.FunSuite

class CollisionProcessorTest extends FunSuite {
  test("calculateNewLocation should calculate new location correctly") {
    val playerData = PlayerData(uid = "player1", sid = "sid1", playerName = "Player 1", locX = 10, locY = 10, color = "red", radius = 5)
    val tickData = TickData(uid = "player1", xVector = 1, yVector = -1)
    val speed = 0.1

    val (newLocX, newLocY) = calculateNewLocation(playerData, tickData, speed)

    assertEquals(newLocX, 10.1)
    assertEquals(newLocY, 10.1)
  }

  test(
    "isOrbCollidingWithPlayer should return true when orb collides with player"
  ) {
    val pData = PlayerData(
      uid = "player1",
      sid = "sid1",
      playerName = "John",
      locX = 10.0,
      locY = 10.0,
      color = "blue",
      radius = 5.0
    )
    val orb = OrbData("orb1", "red", 15, 15, 3)

    val result = isOrbCollidingWithPlayer(pData, orb)

    assertEquals(result, true)
  }

  test(
    "isOrbCollidingWithPlayer should return false when orb does not collide with player"
  ) {
    val pData = PlayerData(
      uid = "player1",
      sid = "sid1",
      playerName = "John",
      locX = 10.0,
      locY = 10.0,
      color = "blue",
      radius = 5
    )
    val orb =
      OrbData(uid = "orb1", color = "red", locX = 25, locY = 25, radius = 3)

    val result = isOrbCollidingWithPlayer(pData, orb)

    assertEquals(result, false)
  }

  test("isAABBTestPassing should return true when AABB test passes") {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "red", 15, 15, 3)

    val result = isAABBTestPassing(pData, orb)

    assertEquals(result, true)
  }

  test("isAABBTestPassing should return false when AABB test fails") {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "green", 25, 25, 3)

    val result = isAABBTestPassing(pData, orb)

    assertEquals(result, false)
  }

  test(
    "isPythagorasTestPassing should return true when Pythagoras test passes"
  ) {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "red", 13, 13, 3)

    val result = isPythagorasTestPassing(pData, orb)

    assertEquals(result, true)
  }

  test(
    "isPythagorasTestPassing should return false when Pythagoras test fails"
  ) {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "green", 25, 25, 3)

    val result = isPythagorasTestPassing(pData, orb)

    assertEquals(result, false)
  }
}
