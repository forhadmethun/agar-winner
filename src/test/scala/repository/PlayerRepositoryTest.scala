package repository

import cats.effect.IO
import core.CollisionProcessor
import cats.effect.unsafe.implicits.global
import models.*
import munit.FunSuite
import org.scalacheck.Gen
import repository.PlayerRepository
import org.scalacheck.Prop.forAll
import util.PlayerTestBase
class PlayerRepositoryTest extends FunSuite with PlayerTestBase {

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
      val player = Player(playerConfig, playerData)
      repository.update(player).unsafeRunSync()
      val result = repository.get(playerId).unsafeRunSync()
      result.contains(player)
    }
    getPlayerProp.check()
  }

  test("getAll should return all players in the repository") {
    val getAllProp = forAll(Gen.listOfN(3,playerDataGen), playerConfigGen) { (playerDataList, playerConfig) =>
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      val players = playerDataList.map(playerData => Player(playerConfig, playerData))
      players.foreach(p => repository.update(p).unsafeRunSync())
      val result = repository.getAll.unsafeRunSync()
      result == players.toVector
    }
    getAllProp.check()
  }

  test("update should update the player in the repository") {
    val props = forAll(playerDataGen, playerConfigGen) { (playerData, playerConfig) =>
      val player: Player = Player(playerConfig, playerData)
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      repository.update(player).unsafeRunSync()
      val result = repository.get(player.playerData.uid).unsafeRunSync()
      result.contains(player)
    }
    props.check()
  }

  test("delete should remove the player from the repository") {
    val props = forAll(playerDataGen, playerConfigGen) { (playerData, playerConfig) =>
      val player: Player = Player(playerConfig, playerData)
      val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
      repository.update(player).unsafeRunSync()
      repository.delete(player.playerData.uid).unsafeRunSync()
      val result = repository.get(player.playerData.uid).unsafeRunSync()
      result.isEmpty
    }
    props.check()
  }
}
