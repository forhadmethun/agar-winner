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
  def get(uid: String): F[Option[Orb[F]]]
  def getAll: F[Vector[Orb[F]]]
  def save(orb: Orb[F]): F[Unit]
  def saveAll(data: Vector[Orb[F]]): F[Unit]

object OrbRepository:
  def inMemory[F[_]: Sync]: F[OrbRepository[F]] =
    Ref[F].of(Vector.empty[Orb[F]]).map { ref =>
      new OrbRepository[F]:
        def get(uid: String): F[Option[Orb[F]]] =
          ref.get.map(_.find(_.orbData.uid == uid))
        def getAll: F[Vector[Orb[F]]] = ref.get
        def save(orb: Orb[F]): F[Unit] =
          ref.update(
            _.filterNot(_.orbData.uid == orb.orbData.uid) :+ orb
          )
        def saveAll(data: Vector[Orb[F]]): F[Unit] =
          data.traverse_(orbData => save(orbData))
    }
