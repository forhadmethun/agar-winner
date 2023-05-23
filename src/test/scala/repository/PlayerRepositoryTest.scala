package repository

import cats.effect.IO
import core.CollisionProcessor
import cats.effect.unsafe.implicits.global
import models.*
import munit.FunSuite
import org.scalacheck.Gen
import repository.PlayerRepository
import org.scalacheck.Prop.forAll
class PlayerRepositoryTest extends FunSuite {

  val playerConfigGen: Gen[PlayerConfig] = for {
    speed <- Gen.posNum[Double]
    zoom <- Gen.posNum[Double]
  } yield PlayerConfig(speed = speed, zoom = zoom)

  val playerDataGen: Gen[PlayerData] = for {
    playerId <-Gen.nonEmptyListOf(Gen.alphaNumChar).map(_.mkString)
    playerName <- Gen.alphaNumStr
    sid <- Gen.alphaNumStr
    locX <- Gen.posNum[Double]
    locY <- Gen.posNum[Double]
    color <- Gen.alphaStr
    radius <- Gen.posNum[Double]
  } yield PlayerData(playerId, sid, playerName, locX, locY, color, radius)


  val playerConfig: PlayerConfig = PlayerConfig(speed = 0, zoom = 0.0)
  val playerData: PlayerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
  val player: Player[IO] = Player[IO](playerConfig, playerData)
  def generatePlayerId: IO[String] = IO(Gen.uuid.sample.get.toString)

  test("get should return None when player with the given ID does not exist") {
    val playerIdGen: Gen[String] = Gen.uuid.sample.get.toString
    val getPlayerProp = forAll(playerIdGen) { playerId =>
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      val result = repository.get(playerId).unsafeRunSync()
      result.isEmpty
    }
    getPlayerProp.check()
  }

  test("get should return the player with the given ID when it exists") {
    val getPlayerProp = forAll(playerConfigGen, playerDataGen) { (playerConfig, playerData) =>
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      val playerId = playerData.uid
      val player = Player[IO](playerConfig, playerData)
      repository.update(player).unsafeRunSync()
      val result = repository.get(playerId).unsafeRunSync()
      result.contains(player)
    }
    getPlayerProp.check()
  }

  test("getAll should return all players in the repository") {
    val getAllProp = forAll(Gen.listOfN(3,playerDataGen)) { playerDataList =>
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      val players = playerDataList.map(playerData => Player[IO](playerConfig, playerData))
      players.foreach(p => repository.update(p).unsafeRunSync())
      val result = repository.getAll.unsafeRunSync()
      result == players.toVector
    }
    getAllProp.check()
  }

  test("update should update the player in the repository") {
    val props = forAll { (updatedColor: String) =>
      val updatedPlayerData: PlayerData = playerData.copy(color = updatedColor)
      val updatedPlayer: Player[IO] = player.copy(playerData = updatedPlayerData)
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      repository.update(updatedPlayer).unsafeRunSync()
      val result = repository.get(updatedPlayerData.uid).unsafeRunSync()
      result.contains(updatedPlayer)
    }
    props.check()
  }

  test("delete should remove the player from the repository") {
    val props = forAll(playerDataGen, playerConfigGen) { (playerData, playerConfig) =>
      val player: Player[IO] = Player[IO](playerConfig, playerData)
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      repository.update(player).unsafeRunSync()
      repository.delete(player.playerData.uid).unsafeRunSync()
      val result = repository.get(player.playerData.uid).unsafeRunSync()
      result.isEmpty
    }
    props.check()
  }
}
