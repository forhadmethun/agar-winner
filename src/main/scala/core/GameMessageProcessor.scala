package core

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.all.*
import models.OrbData.*
import io.circe.parser.*
import io.circe.syntax.*
import models.PlayerData.*
import models.PlayerConfig.*
import models.*
import models.Settings.*
import services.{OrbService, PlayerService}

trait GameMessageProcessor[F[_]]:
  def processInitMessage(initMsg: Request with Request.InitMessage): F[GameMessage]
  def processTickMessage(tickMsg: Request with Request.TickMessage): F[GameMessage]

object GameMessageProcessor:
  def create[F[_]: Sync](
      playerService: PlayerService[F],
      orbService: OrbService[F],
      collisionProcessor: CollisionProcessor[F]
  ): GameMessageProcessor[F] = new GameMessageProcessor[F] {
    def processInitMessage(initMsg: Request with Request.InitMessage): F[GameMessage] = {
      for {
        playerData <- PlayerData
          .createPlayerData[F](initMsg.data.playerName, initMsg.data.sid)
        playerConfig = PlayerConfig(speed = defaultSpeed, zoom = defaultZoom)
        orbs <- orbService.getAllOrbs
        _ <- playerService.savePlayer(playerConfig, playerData)
        msg <- InitMessageResponseData
          .create[F](orbs.map(_.orbData), playerData)
          .map(data =>
            GameMessage(
              Response
                .InitMessageResponse(data)
                .asInstanceOf[Response]
                .asJson
                .toString
            )
          )
      } yield msg
    }

    def processTickMessage(tickMsg: Request with Request.TickMessage): F[GameMessage] = {
      for {
        player <- playerService.getPlayer(tickMsg.data.uid)
        pConfig = player.playerConfig.copy(
          xVector = tickMsg.data.xVector,
          yVector = tickMsg.data.yVector
        )
        (newLocX, newLocY) = CollisionProcessor.calculateNewLocation(
          player.playerData,
          tickMsg.data,
          player.playerConfig.speed
        )
        pData = player.playerData.copy(locX = newLocX, locY = newLocY)
        oData <- orbService.getAllOrbs
        _ <- playerService.savePlayer(pConfig, pData)
        _ <- collisionProcessor.checkAndProcessOrbCollision(pData.uid)
        _ <- collisionProcessor.checkAndProcessPlayerCollisions(pData.uid)
        msg = GameMessage(
          Response
            .TickMessageResponse(TickMessageResponseData(pData, oData.map(_.orbData)))
            .asInstanceOf[Response]
            .asJson
            .toString
        )
      } yield msg
    }
  }
