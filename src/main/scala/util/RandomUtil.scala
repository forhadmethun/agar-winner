package util

import cats.effect.Sync
import cats.syntax.all.*
import cats.effect.std.Random

object RandomUtil:
  private val maxColorValue = 256
  def getRandomColor[F[_]: Sync]: F[String] =
    for {
      random <- Random.scalaUtilRandom[F]
      r <- random.nextIntBounded(maxColorValue)
      g <- random.nextIntBounded(maxColorValue)
      b <- random.nextIntBounded(maxColorValue)
    } yield s"rgb($r, $g, $b)"

  def getRandomInt[F[_] : Sync](n: Int): F[Int] =
    Random.scalaUtilRandom[F].flatMap(_.nextIntBounded(n))
