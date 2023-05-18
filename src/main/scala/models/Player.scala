package models

import cats.effect.IO
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import io.circe.Codec
import models.Settings.*
import util.ColorGenerator

import java.util.UUID
import scala.util.Random

case class PlayerConfig
(
  xVector: Double = 0,
  yVector: Double = 0,
  speed: Double,
  zoom: Double
)

object PlayerConfig {
  def updatePlayerConfig(playerConfig: PlayerConfig): PlayerConfig = {
    playerConfig.copy(
      zoom = if (playerConfig.zoom > zoomThreshold) playerConfig.zoom - zoomValueChange else playerConfig.zoom,
      speed = if (playerConfig.speed < -speedThreshold) playerConfig.speed + speedValueChange
      else if (playerConfig.speed > speedThreshold) playerConfig.speed - speedValueChange
      else playerConfig.speed
    )
  }
}

case class PlayerData(
    uid: String,
    sid: String,
    playerName: String,
    locX: Double,
    locY: Double,
    color: String,
    radius: Double,
    score: Int = 0,
    orbsAbsorbed: Int = 0
) extends CircularShape derives Codec.AsObject

object PlayerData {
  def createPlayerData[F[_]: Sync](playerName: String, sid: String): F[PlayerData] = {
    for {
      uid <- Sync[F].delay(UUID.randomUUID().toString)
      locX <- Sync[F].delay(Random.nextInt(worldWidth))
      locY <- Sync[F].delay(Random.nextInt(worldWidth))
      color <- ColorGenerator.getRandomColor[F]
      radius = defaultSize
    } yield PlayerData(uid, sid, playerName, locX, locY, color, radius)
  }

  def updatePlayerData(playerData: PlayerData): PlayerData = {
    playerData.copy(
      score = playerData.score + 1,
      orbsAbsorbed = playerData.orbsAbsorbed + 1,
      radius = playerData.radius + radiusValueChange
    )
  }
}

case class Player[F[_]](playerConfig: PlayerConfig, playerData: PlayerData)