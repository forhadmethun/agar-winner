package services

import cats.*
import cats.effect.*
import cats.effect.{IO, SyncIO}
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import io.circe.parser.*
import io.circe.syntax.*
import fs2.{Stream, *}
import core.GameServer
import models.*
import models.OrbData.*
import models.PlayerData.*
import models.PlayerConfig.*
import models.Settings.*
import org.http4s.websocket.WebSocketFrame
import services.GameService.*
import scala.concurrent.duration.*

final class GameService[F[_]](
    val game: F[GameServer[F]],
    val playerService: PlayerService[F],
    val orbService: OrbService[F]
):
  def subscribe: Stream[F, GameMessage] =
    Stream.eval(game).flatMap(_.subscribe)

  def publish(m: GameMessage)(using MonadThrow[F]): F[Unit] =
    game.flatMap(_.publish(m))

  protected def deamon(using Concurrent[F]): F[Unit] =
    game.flatMap(g => Spawn[F].start(g.daemon).void)

  def extractAndProcessMessage(text: String)(using Async[F]): F[GameMessage] = {
    for {
      req <- MonadThrow[F].fromEither(decode[Request](text))
      msg <- req match {
        case initMsg: Request.InitMessage => processInitMessage(initMsg)
        case tickMsg: Request.TickMessage => processTickMessage(tickMsg)
      }
    } yield msg
  }

  private def processInitMessage(initMsg: Request with Request.InitMessage)(using Async[F]) = {
    for {
      playerData <- PlayerData.createPlayerData[F](initMsg.data.playerName, initMsg.data.sid)
      playerConfig = PlayerConfig(speed = defaultSpeed, zoom = defaultZoom)
      orbs <- orbService.getAllOrbs
      _ <- playerService.savePlayer(playerConfig, playerData)
      msg <- InitMessageResponseData
        .create[F](orbs.map(_.orbData), playerData)
        .map(data =>
          GameMessage(Response.InitMessageResponse(data).asInstanceOf[Response].asJson.toString)
        )
    } yield msg
  }

  private def processTickMessage(tickMsg: Request with Request.TickMessage)(using Async[F]) = {
    for {
      player <- playerService.getPlayer(tickMsg.data.uid)
      pConfig = player.playerConfig.copy(xVector = tickMsg.data.xVector, yVector = tickMsg.data.yVector)
      (newLocX, newLocY) = calculateNewLocation(player.playerData, tickMsg.data, player.playerConfig.speed)
      pData = player.playerData.copy(locX = newLocX, locY = newLocY)
      oData <- orbService.getAllOrbs
      _ <- playerService.savePlayer(pConfig, pData)
      _ <- checkForOrbCollision(pData.uid)
      _ <- checkForPlayerCollisions(pData.uid)
      msg = GameMessage(Response.TickMessageResponse(TickMessageResponseData(pData, oData.map(_.orbData)))
        .asInstanceOf[Response].asJson.toString)
    } yield msg
  }
  private def checkForOrbCollision(uid: String)(using Async[F]) = {
    for {
      player <- playerService.getPlayer(uid)
      orbs <- orbService.getAllOrbs
      collisionOrbOpt = orbs.map(_.orbData)
        .find(orb => isOrbCollidingWithPlayer(player.playerData, orb))
      result <- handleCollisionOrb(collisionOrbOpt, player)
    } yield result
  }
  private def checkForPlayerCollisions(uid: String)(using Async[F]) = for {
      player <- playerService.getPlayer(uid)
      players <- playerService.getAllPlayers
      collisionPlayerOpt = players.filter(_.playerData.uid != uid)
        .find(p => isOrbCollidingWithPlayer(player.playerData, p.playerData))
      _ <- handleCollisionPlayer(collisionPlayerOpt, player)
    } yield ()
  private def handleCollisionOrb(collisionOrbOpt: Option[OrbData], player: Player[F])(using Async[F]): F[Option[Unit]] = {
    collisionOrbOpt match {
      case Some(collisionOrb) =>
        val updatedPlayerData = updatePlayerData(player.playerData)
        val updatedPConfig = updatePlayerConfig(player.playerConfig)
        for {
          updatedOrb <- createUpdatedOrb(collisionOrb)
          _ <- orbService.saveOrb(updatedOrb)
          _ <- playerService.savePlayer(updatedPConfig, updatedPlayerData)
        } yield Some(())
      case None => Async[F].pure(None)
    }
  }
  private def handleCollisionPlayer(collisionPlayerOpt: Option[Player[F]], player: Player[F])(using Async[F]): F[Option[Unit]] = {
    collisionPlayerOpt match {
      case Some(collisionPlayer) =>
        val (playerToBeUpdated, playerToBeDeleted) =
          if (player.playerData.radius > collisionPlayer.playerData.radius)
            (player, collisionPlayer)
          else
            (collisionPlayer, player)
        val updatedPlayerData = updatePlayerData(playerToBeUpdated.playerData)
        val updatedPlayerConfig = updatePlayerConfig(playerToBeUpdated.playerConfig)
        for {
          _ <- playerService.deletePlayer(playerToBeDeleted.playerData.uid)
          _ <- playerService.savePlayer(updatedPlayerConfig, updatedPlayerData)
        } yield Some(())
      case _ => Async[F].pure(None)
    }
  }
  def playerListStream(using Async[F]): Stream[F, F[WebSocketFrame]] =
    Stream
      .awakeEvery[F](33.milliseconds)
      .flatMap(_ => Stream.eval(playerService.getAllPlayers))
      .map(players => {
        Sync[F]
          .pure(WebSocketFrame.Text(Response.PlayerListMessageResponse(
            players.map(_.playerData)).asInstanceOf[Response].asJson.toString))
      })

object GameService:
  def create[F[_]: Async]: F[GameService[F]] =
    for
      game <- GameServer.create[F]
      players <- PlayerService.create[F]
      orbs <- OrbService.create[F]
      service = GameService(Async[F].pure(game), players, orbs)
      _ <- service.deamon
    yield service
  def calculateNewLocation(playerData: PlayerData, tickData: TickData, speed: Double): (Double, Double) = {
    val currentLocX = playerData.locX
    val currentLocY = playerData.locY
    val xVector = tickData.xVector
    val yVector = tickData.yVector

    val isAtLeftBoundary = currentLocX < 0 && xVector < 0
    val isAtRightBoundary = currentLocX > worldWidth && xVector > 0
    val isAtTopBoundary = currentLocY < 0 && yVector > 0
    val isAtBottomBoundary = currentLocY > worldHeight && yVector < 0

    val newLocX = if (isAtLeftBoundary || isAtRightBoundary) {
      currentLocX
    } else {
      currentLocX + speed * xVector
    }

    val newLocY = if (isAtTopBoundary || isAtBottomBoundary) {
      currentLocY
    } else {
      currentLocY - speed * yVector
    }

    (newLocX, newLocY)
  }
  def isOrbCollidingWithPlayer(pData: PlayerData, collider: CircularShape): Boolean = {
    isAABBTestPassing(pData, collider) && isPythagorasTestPassing(pData, collider)
  }
  def isAABBTestPassing(pData: PlayerData, collider: CircularShape): Boolean = {
    pData.locX + pData.radius + collider.radius > collider.locX &&
      pData.locX < collider.locX + pData.radius + collider.radius &&
      pData.locY + pData.radius + collider.radius > collider.locY &&
      pData.locY < collider.locY + pData.radius + collider.radius
  }
  def isPythagorasTestPassing(pData: PlayerData, collider: CircularShape): Boolean = {
    val distanceSquared = math.pow(pData.locX - collider.locX, 2) +
      math.pow(pData.locY - collider.locY, 2)
    val radiusSumSquared = math.pow(pData.radius + collider.radius, 2)
    distanceSquared < radiusSumSquared
  }
