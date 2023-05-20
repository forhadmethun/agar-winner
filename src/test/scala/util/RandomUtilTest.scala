package util

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import models.OrbData.createUpdatedOrb
import munit.*

import cats.effect.{IO, Sync}
import cats.syntax.all._
import cats.effect.std.Random

class RandomUtilTest extends FunSuite {
  test("should generate a random RGB color string") {
    val result: IO[String] = RandomUtil.getRandomColor[IO]
    result.map(color => {
      assert(color.matches("""rgb\(\d{1,3}, \d{1,3}, \d{1,3}\)"""))
    })
  }
  test("getRandomInt should generate a random integer between 0 and the specified value") {
    val n = 10
    val result: IO[Int] = RandomUtil.getRandomInt[IO](n)
    result.map(number => {
      assert(number >= 0 && number < n)
    })
  }
}
