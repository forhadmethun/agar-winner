package services

import cats.effect.IO
import models.OrbData
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import repository.PlayerRepository
import models.*
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
import util.PlayerTestBase
class PlayerServiceTest extends FunSuite with PlayerTestBase {
  test("getAllPlayers should return all players") {
    val getAllProp = forAll(Gen.listOfN(3, playerDataGen), playerConfigGen) { (playerDataList, playerConfig) =>
      val repo = PlayerRepository.inMemory[IO].unsafeRunSync()
      val service = new PlayerService[IO](repo)
      val players = playerDataList.map(playerData => Player(playerConfig, playerData))
      players.foreach(p => service.savePlayer(p.playerConfig, p.playerData).unsafeRunSync())
      val result = service.getAllPlayers.unsafeRunSync()
      result == players.toVector
    }
    getAllProp.check()
  }

  test("getPlayer should return a player with the given ID") {
    val getPlayerProp = forAll(playerDataGen, playerConfigGen) { (playerData, playerConfig) =>
      val repo = PlayerRepository.inMemory[IO].unsafeRunSync()
      val service = new PlayerService(repo)
      service.savePlayer(playerConfig, playerData).unsafeRunSync()
      val result = service.getPlayer(playerData.uid).unsafeRunSync()
      result.playerData == playerData && result.playerConfig == playerConfig
    }
    getPlayerProp.check()
  }

  test("getPlayer should throw an exception when no player is found with the given ID") {
    val playerIdGen: Gen[String] = Gen.alphaNumStr
    val getPlayerProp = forAll(playerIdGen) { playerId =>
      val repo = PlayerRepository.inMemory[IO].unsafeRunSync()
      val service = new PlayerService(repo)
      val result = service.getPlayer(playerId).attempt.unsafeRunSync()
      result.isLeft && result.isInstanceOf[Left[Throwable, _]] &&
        result.swap.toOption.get.isInstanceOf[IllegalArgumentException] &&
        result.swap.toOption.get.getMessage == s"No player found with id: $playerId"
    }
    getPlayerProp.check()
  }

  test("savePlayer should return the saved player") {
    val savePlayerProp = forAll(playerDataGen, playerConfigGen) { (playerData, playerConfig) =>
      val repo = PlayerRepository.inMemory[IO].unsafeRunSync()
      val service = new PlayerService(repo)
      service.savePlayer(playerConfig, playerData).unsafeRunSync()
      val result = service.savePlayer(playerConfig, playerData).unsafeRunSync()
      result.playerConfig == playerConfig && result.playerData == playerData
    }
    savePlayerProp.check()
  }

  test("deletePlayer should delete the player") {
    val playerIdProp = forAll(playerDataGen, playerConfigGen) { (playerData, playerConfig) =>
      val repo = PlayerRepository.inMemory[IO].unsafeRunSync()
      val service = new PlayerService(repo)
      service.savePlayer(playerConfig, playerData).unsafeRunSync()
      service.deletePlayer(playerData.uid).unsafeRunSync()
      val result = service.getAllPlayers.unsafeRunSync()
      result.isEmpty
    }
    playerIdProp.check()
  }

  test("create should return a PlayerService instance") {
    val result = PlayerService.create[IO].unsafeRunSync()
    assert(result.isInstanceOf[PlayerService[IO]])
  }
}
