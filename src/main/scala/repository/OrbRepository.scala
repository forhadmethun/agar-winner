package repository

import cats.*
import cats.effect.*
import cats.syntax.all.*
import io.circe.*
import io.circe.syntax.*
import models.Orb
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*

trait OrbRepository[F[_]]:
  def get(uid: String): F[Option[Orb]]
  def getAll: F[Vector[Orb]]
  def save(orb: Orb): F[Unit]
  def saveAll(data: Vector[Orb]): F[Unit]

object OrbRepository:
  def inMemory[F[_]: Sync]: F[OrbRepository[F]] =
    Ref[F].of(Map.empty[String, Orb]).map { ref =>
      new OrbRepository[F]:
        def get(uid: String): F[Option[Orb]] =
          ref.get.map(_.get(uid))
        def getAll: F[Vector[Orb]] =
          ref.get.map(_.values.toVector)
        def save(orb: Orb): F[Unit] =
          ref.update(_.updated(orb.orbData.uid, orb))
        def saveAll(data: Vector[Orb]): F[Unit] =
          ref.update(_.++(data.map(orb => orb.orbData.uid -> orb)))
    }
