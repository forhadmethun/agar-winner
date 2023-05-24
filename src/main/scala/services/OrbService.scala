package services

import cats.*
import cats.effect.*
import cats.effect.std.Queue
import cats.implicits.*
import cats.syntax.all.*
import fs2.concurrent.Topic
import io.circe.*
import fs2.*
import models.{Orb, OrbData}
import repository.OrbRepository

final class OrbService[F[_]](val repo: OrbRepository[F]):
  def getAllOrbs: F[Vector[Orb]] =
    repo.getAll

  def saveOrb(orbData: OrbData)(using Monad[F]): F[Orb] =
    val orb = Orb(orbData)
    repo.save(orb).as(orb)

  def saveAllOrb(orbs: Vector[Orb])(using Monad[F]): F[Vector[Orb]] =
    repo.saveAll(orbs).as(orbs)

  def getOrb(uid: String)(using MonadThrow[F]): F[Orb] =
    repo
      .get(uid)
      .flatMap(
        MonadThrow[F]
          .fromOption(_, new IllegalArgumentException(s"No orb found with id: $uid"))
      )

object OrbService:
  def create[F[_]: Sync]: F[OrbService[F]] =
    for
      repo <- OrbRepository.inMemory[F]
      orbs <- Orb.generateOrbs[F]
      _ <- repo.saveAll(orbs)
      service = OrbService(repo)
    yield service
