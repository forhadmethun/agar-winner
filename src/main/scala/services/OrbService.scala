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

final class OrbService[F[_]](
    val repo: OrbRepository[F]
):
  def getAllOrbs: F[Vector[Orb[F]]] =
    repo.getAll

  def saveOrb(orb: OrbData)(using Concurrent[F]): F[Orb[F]] = {
    val player = Orb[F](orb)
    for _ <- repo.save(player)
      yield player
  }


object OrbService:
  def create[F[_]: Async]: F[OrbService[F]] =
    for
      repo <- OrbRepository.inMemory[F]
      orbs <- Orb.generateOrbs[F]
      _ <- repo.saveAll(orbs)
      service = OrbService(repo)
    yield service
