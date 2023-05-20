package models

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import models.*
import models.Settings.*
import models.Player.*
import models.PlayerConfig.updatePlayerConfig
import models.PlayerData.*
import munit.*

class PlayerTest extends FunSuite {
  test("updatePlayerData should correctly update the player data") {
    val playerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val updatedPlayerData = updatePlayerData(playerData)
    assertEquals(updatedPlayerData.score, playerData.score + 1)
    assertEquals(updatedPlayerData.orbsAbsorbed, playerData.orbsAbsorbed + 1)
    assertEquals(
      updatedPlayerData.radius,
      playerData.radius + radiusValueChange
    )
  }

  test("updatePlayerConfig should correctly update the player config") {
    val playerConfig = PlayerConfig(speed = 1.5, zoom = 1.5)
    val updatedPlayerConfig = updatePlayerConfig(playerConfig)
    assertEquals(updatedPlayerConfig.zoom, playerConfig.zoom - zoomValueChange)
    assertEquals(
      updatedPlayerConfig.speed,
      playerConfig.speed - speedValueChange
    )
  }

  test(
    "updatePlayerConfig should maintain player config when no update is needed"
  ) {
    val playerConfig = PlayerConfig(speed = 0, zoom = 0.0)
    val updatedPlayerConfig = updatePlayerConfig(playerConfig)
    assertEquals(updatedPlayerConfig.zoom, playerConfig.zoom)
    assertEquals(updatedPlayerConfig.speed, playerConfig.speed)
  }

}
