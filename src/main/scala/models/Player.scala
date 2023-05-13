package models

import cats.effect.IO
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import io.circe.Codec
import models.Settings.*

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
    playerName: String,
    locX: Double,
    locY: Double,
    color: String,
    radius: Double,
    score: Int = 0,
    orbsAbsorbed: Int = 0
) derives Codec.AsObject

object PlayerData {
  def createPlayerData[F[_]: Sync](playerName: String): F[PlayerData] = {
    for {
      uid <- Sync[F].delay(java.util.UUID.randomUUID().toString)
      locX <- Sync[F].delay(scala.util.Random.nextInt(worldWidth) + 100)
      locY <- Sync[F].delay(scala.util.Random.nextInt(worldWidth) + 100)
      color <- getRandomColor[F]
      radius = defaultSize
    } yield PlayerData(uid, playerName, locX, locY, color, radius)
  }

  def increaseScore(playerData: PlayerData): PlayerData = {
    playerData.copy(score = playerData.score + 1)
  }

  def increaseOrbsAbsorbed(playerData: PlayerData): PlayerData = {
    playerData.copy(orbsAbsorbed = playerData.orbsAbsorbed + 1)
  }

  private def getRandomColor[F[_]: Sync]: F[String] = {
    for {
      r <- Sync[F].delay(scala.util.Random.nextInt(150) + 50)
      g <- Sync[F].delay(scala.util.Random.nextInt(150) + 50)
      b <- Sync[F].delay(scala.util.Random.nextInt(150) + 50)
    } yield s"rgb($r, $g, $b)"
  }

  def updatePlayerData(playerData: PlayerData): PlayerData = {
    playerData.copy(
      score = playerData.score + 1,
      orbsAbsorbed = playerData.orbsAbsorbed + 1,
      radius = playerData.radius + radiusValueChange
    )
  }
}

case class Player[F[_]](playerConfig: PlayerConfig, playerData: PlayerData):
  def getData: PlayerData = playerData
