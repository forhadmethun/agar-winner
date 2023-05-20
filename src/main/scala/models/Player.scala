package models

import cats.effect.Sync
import cats.effect.std.UUIDGen
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import io.circe.Codec
import models.Settings.*
import util.RandomUtil.*

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
      uid <- UUIDGen.randomString[F]
      color <- getRandomColor[F]
      locX <- getRandomInt[F](worldWidth)
      locY <- getRandomInt[F](worldHeight)
    } yield PlayerData(uid, sid, playerName, locX, locY, color, defaultSize)
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