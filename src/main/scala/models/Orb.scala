package models
import io.circe.Codec
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import models.Settings._

case class OrbData(uid: String, color: String, locX: Double, locY: Double, radius: Double)
  extends CircularShape derives Codec.AsObject

object OrbData {
  def apply[F[_]: Sync](): F[OrbData] = {
    for {
      uid <- Sync[F].delay(java.util.UUID.randomUUID().toString)
      color <- getRandomColor[F]
      locX <- Sync[F].delay(scala.util.Random.nextInt(worldWidth))
      locY <- Sync[F].delay(scala.util.Random.nextInt(worldHeight))
    } yield OrbData(uid, color, locX, locY, defaultOrbRadius)
  }

  def apply[F[_] : Sync](uid: String): F[OrbData] = {
    for {
      color <- getRandomColor[F]
      locX <- Sync[F].delay(scala.util.Random.nextInt(worldWidth))
      locY <- Sync[F].delay(scala.util.Random.nextInt(worldHeight))
    } yield OrbData(uid, color, locX, locY, defaultOrbRadius)
  }

  private def getRandomColor[F[_]: Sync]: F[String] = {
    for {
      r <- Sync[F].delay(scala.util.Random.nextInt(151) + 50)
      g <- Sync[F].delay(scala.util.Random.nextInt(151) + 50)
      b <- Sync[F].delay(scala.util.Random.nextInt(151) + 50)
    } yield s"rgb($r, $g, $b)"
  }

  def createUpdatedOrb[F[_] : Sync](collisionOrb: OrbData): F[OrbData] = {
    OrbData[F](collisionOrb.uid)
  }
}

case class Orb[F[_]](orbData: OrbData)

object Orb:
  def generateOrbs[F[_] : Sync]: F[Vector[Orb[F]]] = {
    Vector.fill(defaultOrbs)(OrbData[F]()).sequence.map(_.map(Orb(_)))
  }

