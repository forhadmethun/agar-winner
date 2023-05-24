package repository

import cats.*
import cats.effect.*
import cats.syntax.all.*
import io.circe.*
import io.circe.syntax.*
import models.Player
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*

trait PlayerRepository[F[_]]:
  def get(uid: String): F[Option[Player]]
  def getAll: F[Vector[Player]]
  def update(player: Player): F[Unit]
  def delete(uid: String): F[Unit]

object PlayerRepository:
  def inMemory[F[_]: Sync]: F[PlayerRepository[F]] =
    Ref[F].of(Map.empty[String, Player]).map { ref =>
      new PlayerRepository[F]:
        def get(uid: String): F[Option[Player]] =
          ref.get.map(_.get(uid))
        def getAll: F[Vector[Player]] =
          ref.get.map(_.values.toVector)
        def update(player: Player): F[Unit] =
          ref.update(_.updated(player.playerData.uid, player))
        def delete(uid: String): F[Unit] =
          ref.update(_ - uid)
    }
