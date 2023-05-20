package util

import cats.syntax.all.*
import cats.effect.Sync

import scala.util.Random

object ColorGenerator {
  def getRandomColor[F[_]: Sync]: F[String] = {
    val maxColorValue = 256
    for {
      r <- Sync[F].delay(Random.nextInt(maxColorValue))
      g <- Sync[F].delay(Random.nextInt(maxColorValue))
      b <- Sync[F].delay(Random.nextInt(maxColorValue))
    } yield s"rgb($r, $g, $b)"
  }
}
