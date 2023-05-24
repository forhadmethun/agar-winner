package core

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.all.*
import core.LocationProcessor.*
import models.OrbData.*
import io.circe.parser.*
import io.circe.syntax.*
import models.PlayerData.*
import models.PlayerConfig.*
import models.*
import models.Settings.*
import services.{OrbService, PlayerService}

trait GameMessageProcessor[F[_]]:
  def processInitMessage(initMsg: Request.InitMessage): F[GameMessage]
  def processTickMessage(tickMsg: Request.TickMessage): F[GameMessage]

object GameMessageProcessor:
  def create[F[_]: Sync](
      playerService: PlayerService[F],
      orbService: OrbService[F],
      collisionProcessor: CollisionProcessor[F]
  ): GameMessageProcessor[F] = new GameMessageProcessor[F] {
    def processInitMessage(initMsg: Request.InitMessage): F[GameMessage] = {
      for
        playerData <- PlayerData.createPlayerData[F](initMsg.data.playerName, initMsg.data.sid)
        playerConfig = PlayerConfig(speed = defaultSpeed, zoom = defaultZoom)
        orbs <- orbService.getAllOrbs
        _ <- playerService.savePlayer(playerConfig, playerData)
        initMessageResponse = Response.InitMessageResponse(InitMessageResponseData(orbs.map(_.orbData), playerData))
        msg = GameMessage(initMessageResponse.asJson.toString)
      yield msg
    }

    def processTickMessage(tickMsg: Request.TickMessage): F[GameMessage] = {
      for
        player <- playerService.getPlayer(tickMsg.data.uid)
        (newLocX, newLocY) = calculateNewLocation(player.playerData, tickMsg.data, player.playerConfig.speed)
        updatedPlayer <- playerService.savePlayer(
          player.playerConfig.copy(xVector = tickMsg.data.xVector, yVector = tickMsg.data.yVector),
          player.playerData.copy(locX = newLocX, locY = newLocY)
        )
        allOrbs <- orbService.getAllOrbs
        allPlayers <- playerService.getAllPlayers
        _ <- collisionProcessor.checkAndProcessOrbCollision(updatedPlayer, allOrbs)
        _ <- collisionProcessor.checkAndProcessPlayerCollisions(updatedPlayer, allPlayers)
        responseMessage = Response.TickMessageResponse(TickMessageResponseData(updatedPlayer.playerData, allOrbs.map(_.orbData)))
        msg = GameMessage(responseMessage.asJson.toString)
      yield msg
    }
  }
