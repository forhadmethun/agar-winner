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
import models.*
import models.Settings.*
import org.http4s.websocket.WebSocketFrame
import scala.concurrent.duration.*

final class GameService[F[_]](
    val game: F[Game[F]],
    val playerService: PlayerService[F]
):
  def subscribe: Stream[F, GameMessage] =
    Stream.eval(game).flatMap(_.subscribe)

  def publish(m: GameMessage)(using MonadThrow[F]): F[Unit] =
    game.flatMap(_.publish(m))

  protected def deamon(using Concurrent[F]): F[Unit] =
    game.flatMap(g => Spawn[F].start(g.deamon).void)

  def extractMessage(text: String)(using Async[F]): F[GameMessage] = {
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
      playerData <- PlayerData.createPlayerData[F](initMsg.data.playerName)
      playerConfig = PlayerConfig(speed = defaultSpeed, zoom = defaultZoom)
      _ <- playerService.createPlayer(
        playerConfig,
        playerData
      )
      msg <- InitMessageResponseData
        .create[F](playerData)
        .map(data =>
          GameMessage(Response.InitMessageResponse(data).asInstanceOf[Response].asJson.toString)
        )
    } yield msg
  }

  private def processTickMessage(tickMsg: Request with Request.TickMessage)(using Async[F]) = {
    for {
      player <- playerService.getPlayer(tickMsg.data.uid)
      pConfig = player.playerConfig.copy(xVector = tickMsg.data.xVector, yVector = tickMsg.data.yVector)
      speed = player.playerConfig.speed
      (newLocX, newLocY) = if ((player.playerData.locX < 5 && tickMsg.data.xVector < 0) ||
        (player.playerData.locX > worldWidth && tickMsg.data.xVector > 0)) {
        (player.playerData.locX, player.playerData.locY - speed * tickMsg.data.yVector)
      } else if ((player.playerData.locY < 5 && tickMsg.data.yVector > 0) ||
        (player.playerData.locY > worldHeight && tickMsg.data.yVector < 0)) {
        (player.playerData.locX + speed * tickMsg.data.xVector, player.playerData.locY)
      } else {
        (player.playerData.locX + speed * tickMsg.data.xVector, player.playerData.locY - speed * tickMsg.data.yVector)
      }

      pData = player.playerData.copy(locX = newLocX, locY = newLocY)
      _ <- playerService.createPlayer(pConfig, pData)
      msg <- Sync[F].pure(GameMessage(
        Response.TickMessageResponse(pData).asInstanceOf[Response].asJson.toString))
    } yield msg
  }

  def playerListStream(using Async[F]): Stream[F, F[WebSocketFrame]] =
    Stream
      .awakeEvery[F](33.milliseconds)
      .flatMap(_ => Stream.eval(playerService.getAllPlayers))
      .map(players => {
        Sync[F]
          .pure(WebSocketFrame.Text(Response.PlayerListMessageResponse(
            players.map(q => q.getData)).asInstanceOf[Response].asJson.toString))
      })

object GameService:
  def create[F[_]: Async]: F[GameService[F]] =
    for
      game <- Game.create[F]
      players <- PlayerService.create[F]
      service = GameService(Async[F].pure(game), players)
      _ <- service.deamon
    yield service

final class Game[F[_]](
    private val topic: Topic[F, GameMessage],
    private val q: Queue[F, Option[GameMessage]]
):
  def publish(m: GameMessage): F[Unit] = q.offer(Some(m))

  def subscribe: Stream[F, GameMessage] = topic.subscribe(100)

  def stop: F[Unit] = q.offer(None)

  def deamon(using Concurrent[F]): F[Unit] =
    Stream
      .fromQueueNoneTerminated(q)
      .through(topic.publish)
      .compile
      .drain

object Game:
  def create[F[_]: Concurrent]: F[Game[F]] =
    for
      q <- Queue.unbounded[F, Option[GameMessage]]
      topic <- Topic[F, GameMessage]
    yield Game(topic, q)

final case class GameMessage(content: String)
