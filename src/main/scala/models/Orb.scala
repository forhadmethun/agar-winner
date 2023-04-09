package models
import io.circe.Codec
import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.unsafe.implicits.global
import models.Settings._

case class Orb(color: String, locX: Int, locY: Int, radius: Int)
    derives Codec.AsObject

object Orb {
  def apply[F[_]: Sync](worldWidth: Int, worldHeight: Int): F[Orb] = {
    for {
      color <- getRandomColor[F]
      locX <- Sync[F].delay(scala.util.Random.nextInt(worldWidth))
      locY <- Sync[F].delay(scala.util.Random.nextInt(worldHeight))
    } yield Orb(color, locX, locY, defaultOrbRadius)
  }

  private def getRandomColor[F[_]: Sync]: F[String] = {
    for {
      r <- Sync[F].delay(scala.util.Random.nextInt(151) + 50)
      g <- Sync[F].delay(scala.util.Random.nextInt(151) + 50)
      b <- Sync[F].delay(scala.util.Random.nextInt(151) + 50)
    } yield s"rgb($r, $g, $b)"
  }

  def generateOrbs[F[_]: Sync]: F[Vector[Orb]] = {
    Vector.fill(defaultOrbs)(Orb[F](worldWidth, worldHeight)).sequence
  }
}
