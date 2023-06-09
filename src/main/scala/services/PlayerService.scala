package services

import cats.*
import cats.implicits.*
import cats.effect.*
import cats.effect.std.Queue
import cats.syntax.all.*
import repository.PlayerRepository
import models.{Player, PlayerConfig, PlayerData}

final class PlayerService[F[_]](val repo: PlayerRepository[F]):
  def getAllPlayers: F[Vector[Player]] =
    repo.getAll

  def getPlayer(uid: String)(using MonadThrow[F]): F[Player] =
    repo
      .get(uid)
      .flatMap(
        MonadThrow[F]
          .fromOption(_, new IllegalArgumentException(s"No player found with id: $uid"))
      )

  def savePlayer(playerConfig: PlayerConfig, playerData: PlayerData)(using Monad[F]): F[Player] =
    val player = Player(playerConfig, playerData)
    repo.update(player).as(player)

  def deletePlayer(uid: String): F[Unit] =
    repo.delete(uid)

object PlayerService:
  def create[F[_]: Sync]: F[PlayerService[F]] =
    for
      repo <- PlayerRepository.inMemory[F]
      service = PlayerService(repo)
    yield service
