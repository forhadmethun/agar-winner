package util

import models.*
import org.scalacheck.Gen

trait PlayerTestBase {
  val playerConfigGen: Gen[PlayerConfig] = for
    speed <- Gen.posNum[Double]
    zoom <- Gen.posNum[Double]
  yield PlayerConfig(speed = speed, zoom = zoom)

  val playerDataGen: Gen[PlayerData] = for
    playerId <- Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
    playerName <- Gen.alphaNumStr
    sid <- Gen.alphaNumStr
    locX <- Gen.posNum[Double]
    locY <- Gen.posNum[Double]
    color <- Gen.alphaStr
    radius <- Gen.posNum[Double]
  yield PlayerData(playerId, sid, playerName, locX, locY, color, radius)
}
