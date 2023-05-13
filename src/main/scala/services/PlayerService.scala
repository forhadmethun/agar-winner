package services

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.std.Queue
import cats.syntax.all.*
import fs2.concurrent.Topic
import io.circe.*
import fs2.*
import repository.PlayerRepository
import models.{Player, PlayerConfig, PlayerData}

final class PlayerService[F[_]](
    val repo: PlayerRepository[F]
):
  def getAllPlayers: F[Vector[Player[F]]] =
    repo.getAll

  def getPlayer(uid: String)(using MonadThrow[F]): F[Player[F]] =
    repo
      .get(uid)
      .flatMap(
        MonadThrow[F]
          .fromOption(_, new IllegalArgumentException(s"No player found with id: $uid "))
      )

  def savePlayer(playerConfig: PlayerConfig, playerData: PlayerData)(using Concurrent[F]): F[Player[F]] = {
    val player = Player[F](playerConfig, playerData)
    for _ <- repo.update(player)
      yield player
  }


object PlayerService:
  def create[F[_]: Async]: F[PlayerService[F]] =
    for
      repo <- PlayerRepository.inMemory[F]
      service = PlayerService(repo)
    yield service
