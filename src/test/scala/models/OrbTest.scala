package models

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import models.OrbData.createUpdatedOrb
import munit.*

class OrbTest extends FunSuite {
  test("createUpdatedOrb should create an updated orb") {
    val collisionOrb = OrbData("orb1", "red", 15, 15, 3)
    val updatedOrb = createUpdatedOrb[IO](collisionOrb).unsafeRunSync()
    assertEquals(updatedOrb.uid, collisionOrb.uid)
  }
}
