package core

import cats.effect.*
import fs2.Stream
import org.http4s.websocket.WebSocketFrame
import io.circe.syntax.*
import models.Response
import services.PlayerService
import scala.concurrent.duration.*

final class GameStreamUpdater[F[_]](val playerService: PlayerService[F]):
  def updatedPlayerList(using Async[F]): Stream[F, WebSocketFrame] =
    Stream
      .awakeEvery[F](33.milliseconds)
      .flatMap(_ => Stream.eval(playerService.getAllPlayers))
      .map(players => {
        val playerListResponse = Response.PlayerListMessageResponse(players.map(_.playerData))
        WebSocketFrame.Text(playerListResponse.asJson.toString)
      })

object GameStreamUpdater:
  def create[F[_]: Async](playerService: PlayerService[F]): GameStreamUpdater[F] =
    new GameStreamUpdater[F](playerService)
    