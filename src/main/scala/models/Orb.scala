package models
import io.circe.Codec
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import models.Settings.*
import util.ColorGenerator

import java.util.UUID
import scala.util.Random

case class OrbData(uid: String, color: String, locX: Double, locY: Double, radius: Double)
  extends CircularShape derives Codec.AsObject

object OrbData {
  def apply[F[_] : Sync](): F[OrbData] = {
    for {
      uid <- Sync[F].delay(UUID.randomUUID().toString)
      orbData <- generateOrbData(uid)
    } yield orbData
  }

  def apply[F[_] : Sync](uid: String): F[OrbData] = generateOrbData(uid)

  def createUpdatedOrb[F[_] : Sync](collisionOrb: OrbData): F[OrbData] = OrbData[F](collisionOrb.uid)

  private def generateOrbData[F[_] : Sync](uid: String): F[OrbData] = {
    for {
      color <- ColorGenerator.getRandomColor[F]
      locX <- Sync[F].delay(Random.nextInt(worldWidth))
      locY <- Sync[F].delay(Random.nextInt(worldHeight))
    } yield OrbData(uid, color, locX, locY, defaultOrbRadius)
  }
}

case class Orb[F[_]](orbData: OrbData)

object Orb:
  def generateOrbs[F[_] : Sync]: F[Vector[Orb[F]]] = {
    Vector.fill(defaultOrbs)(OrbData[F]()).sequence.map(_.map(Orb(_)))
  }

