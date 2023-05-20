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
  val gameServer: F[GameServer[F]],
  val gameStreamUpdater: GameStreamUpdater[F],
  val gameMessageProcessor: GameMessageProcessor[F],
):
  def subscribe: Stream[F, GameMessage] =
    Stream.eval(gameServer).flatMap(_.subscribe)

  def publish(m: GameMessage)(using MonadThrow[F]): F[Unit] =
    gameServer.flatMap(_.publish(m))

  protected def daemon(using Concurrent[F]): F[Unit] =
    gameServer.flatMap(g => Spawn[F].start(g.daemon).void)

  def extractAndProcessMessage(text: String)(using Async[F]): F[GameMessage] = {
    for {
      req <- MonadThrow[F].fromEither(decode[Request](text))
      msg <- req match {
        case initMsg: Request.InitMessage => gameMessageProcessor.processInitMessage(initMsg)
        case tickMsg: Request.TickMessage => gameMessageProcessor.processTickMessage(tickMsg)
      }
    } yield msg
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
      service = GameService(Sync[F].pure(gameServer), gameStreamUpdater, gameMessageProcessor)
      _ <- service.daemon
    yield service
