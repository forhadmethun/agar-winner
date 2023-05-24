package services

import cats.*
import cats.effect.*
import cats.implicits.*
import cats.syntax.all.*
import io.circe.parser.*
import fs2.Stream
import core.*
import models.*

final class GameService[F[_]]
(
  val gameServer: GameServer[F],
  val gameStreamUpdater: GameStreamUpdater[F],
  val gameMessageProcessor: GameMessageProcessor[F]
):
  def subscribe: Stream[F, GameMessage] = gameServer.subscribe

  def publish(msg: GameMessage): F[Unit] = gameServer.publish(msg)

  protected def daemon(using Concurrent[F]): F[Unit] =
    Spawn[F].start(gameServer.daemon).void

  def extractAndProcessMessage(text: String)(using Async[F]): F[GameMessage] = {
    for
      req <- MonadThrow[F].fromEither(decode[Request](text))
      msg <- req match {
        case initMsg: Request.InitMessage => gameMessageProcessor.processInitMessage(initMsg)
        case tickMsg: Request.TickMessage => gameMessageProcessor.processTickMessage(tickMsg)
      }
    yield msg
  }

object GameService:
  def create[F[_]: Async]: F[GameService[F]] =
    for
      gameServer <- GameServer.create[F]
      playerService <- PlayerService.create[F]
      orbService <- OrbService.create[F]
      gameStreamUpdater = GameStreamUpdater.create[F](playerService)
      gameMessageProcessor = GameMessageProcessor.create[F](
        playerService, orbService, CollisionProcessor.create[F](playerService, orbService))
      service = GameService(gameServer, gameStreamUpdater, gameMessageProcessor)
      _ <- service.daemon
    yield service
