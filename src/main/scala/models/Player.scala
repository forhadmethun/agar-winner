package models

import cats.effect.IO
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import models.Settings._
case class PlayerConfig(defaultSpeed: Int, defaultZoom: Double)

case class PlayerData(
    uid: String,
    playerName: String,
    locX: Int,
    locY: Int,
    color: String,
    radius: Int,
    score: Int = 0,
    orbsAbsorbed: Int = 0
)

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
}

case class Player(playerConfig: PlayerConfig, playerData: PlayerData)
