package routes

import cats.effect.*
import cats.effect.kernel.implicits.*
import cats.implicits.*
import fs2.Compiler.Target.forSync
import fs2.concurrent.Topic
import fs2.{Stream, *}
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.server.websocket.{WebSocketBuilder, WebSocketBuilder2}
import org.http4s.websocket.WebSocketFrame
import services.GameService

final class GameRoutes[F[_]](gameService: GameService[F]) extends Http4sDsl[F]:
  def wsRoutes(using Async[F])(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root =>
      val out: Stream[F, WebSocketFrame] = gameService.subscribe.map(msg => {
        WebSocketFrame.Text(msg.content)
      })

      val in: Pipe[F, WebSocketFrame, Unit] =
        _.evalMap(a => Sync[F].delay(scribe.info(a.toString)).as(a))
          .collect { case WebSocketFrame.Text(text, _) =>
            GameService.extractMessage(text)
          }
          .foreach(gameService.publish)
      builder.build(out, in)
    }

object GameRoutes:
  def setup[F[_]: Async]: F[GameRoutes[F]] =
    for service <- GameService.create[F]
    yield GameRoutes[F](service)
