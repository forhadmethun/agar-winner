package services

import cats.*
import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import io.circe.*
import io.circe.generic.auto.*
import io.circe.parser.*
import io.circe.syntax.*
import fs2.{Stream, *}
import models.*
import models.Messages.*

final class GameService[F[_]](
    val game: F[Game[F]]
):
  def subscribe: Stream[F, GameMessage] =
    Stream.eval(game).flatMap(_.subscribe)

  def publish(m: GameMessage)(using MonadThrow[F]): F[Unit] =
    game.flatMap(_.publish(m))

  protected def deamon(using Concurrent[F]): F[Unit] =
    def next: F[Unit] = {
      game.flatMap(game => {
        Spawn[F].start(game.deamon).void
      })
    }
    Spawn[F].start(next).void

object GameService:
  def create[F[_]: Async]: F[GameService[F]] =
    for
      game <- Game.create[F]
      service = GameService(Async[F].pure(game))
      _ <- service.deamon
    yield service

  def extractMessage[F[_]](text: String): GameMessage = {
    decode[InitMessage](text)
      .map { init =>
        GameMessage(
          init.`type`,
          InitMessageResponse(
            "initReturn",
            InitMessageResponseData(Orb.generateOrbs(Settings()))
          ).asJson.toString
        )
      }
      .orElse {
        decode[TickMessage](text).map { tick =>
          GameMessage(
            tick.`type`,
            s"xVector: ${tick.data.xVector}, yVector: ${tick.data.yVector}"
          )
        }
      }
      .fold(
        error => GameMessage("error", s"JSON parsing error: $error"),
        identity
      )
  }

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

final case class GameMessage(id: String, content: String) derives Codec.AsObject
