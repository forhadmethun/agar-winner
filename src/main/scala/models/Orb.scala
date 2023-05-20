package models
import io.circe.Codec
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import models.Settings.*
import cats.effect.std.UUIDGen
import util.RandomUtil.*

case class OrbData(
    uid: String,
    color: String,
    locX: Double,
    locY: Double,
    radius: Double
) extends CircularShape
    derives Codec.AsObject

object OrbData {
  def apply[F[_]: Sync](): F[OrbData] = {
    for {
      uid <- UUIDGen.randomString[F]
      orbData <- generateOrbData(uid)
    } yield orbData
  }

  def apply[F[_]: Sync](uid: String): F[OrbData] = generateOrbData[F](uid)

  def createUpdatedOrb[F[_]: Sync](collisionOrb: OrbData): F[OrbData] =
    OrbData[F](collisionOrb.uid)

  private def generateOrbData[F[_]: Sync](uid: String): F[OrbData] = {
    for {
      color <- getRandomColor[F]
      locX <- getRandomInt[F](worldWidth)
      locY <- getRandomInt[F](worldHeight)
    } yield OrbData(uid, color, locX, locY, defaultOrbRadius)
  }
}

case class Orb[F[_]](orbData: OrbData)

object Orb:
  def generateOrbs[F[_]: Sync]: F[Vector[Orb[F]]] = {
    Vector.fill(defaultOrbs)(OrbData[F]()).traverse(_.map(Orb(_)))
  }
