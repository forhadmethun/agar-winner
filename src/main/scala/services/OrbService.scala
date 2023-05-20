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
  def getAllOrbs: F[Vector[Orb[F]]] =
    repo.getAll

  def saveOrb(orbData: OrbData)(using Monad[F]): F[Orb[F]] =
    val orb = Orb[F](orbData)
    repo.save(orb).as(orb)

object OrbService:
  def create[F[_]: Sync]: F[OrbService[F]] =
    for
      repo <- OrbRepository.inMemory[F]
      orbs <- Orb.generateOrbs[F]
      _ <- repo.saveAll(orbs)
      service = OrbService(repo)
    yield service
