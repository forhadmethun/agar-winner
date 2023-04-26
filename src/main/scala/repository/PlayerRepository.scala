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
  def get(uid: String): F[Option[Player[F]]]
  def getAll: F[Vector[Player[F]]]
  def update(player: Player[F]): F[Unit]

object PlayerRepository:
  def inMemory[F[_]: Sync]: F[PlayerRepository[F]] =
    Ref[F].of(Vector.empty[Player[F]]).map { ref =>
      new PlayerRepository[F]:
        def get(uid: String): F[Option[Player[F]]] =
          ref.get.map(_.find(_.playerData.uid == uid))
        def getAll: F[Vector[Player[F]]] = ref.get
        def update(player: Player[F]): F[Unit] =
          ref.update(
            _.filterNot(_.playerData.uid == player.playerData.uid) :+ player
          )
    }
