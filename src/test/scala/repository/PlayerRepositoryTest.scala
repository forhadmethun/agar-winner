package repository

import cats.effect.IO
import core.CollisionProcessor
import cats.effect.unsafe.implicits.global
import models.*
import munit.FunSuite
import org.scalacheck.Gen
import repository.PlayerRepository

class PlayerRepositoryTest extends FunSuite {
  val playerConfig: PlayerConfig = PlayerConfig(speed = 0, zoom = 0.0)
  val playerData: PlayerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
  val player: Player[IO] = Player[IO](playerConfig, playerData)
  def generatePlayerId: IO[String] = IO(Gen.uuid.sample.get.toString)

  test("get should return None when player with the given ID does not exist") {
    val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
    val playerId = "nonexistent"
    val result = repository.get(playerId).unsafeRunSync()
    assertEquals(result, None)
  }

  test("get should return the player with the given ID when it exists") {
    val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
    val playerId = playerData.uid
    val player = Player[IO](playerConfig, playerData)
    repository.update(player).unsafeRunSync()
    val result = repository.get(playerId).unsafeRunSync()
    assertEquals(result, Some(player))
  }

  test("getAll should return all players in the repository") {
    val repository = PlayerRepository.inMemory[IO].unsafeRunSync()

    val players = Vector(
      Player[IO](playerConfig, playerData),
      Player[IO](playerConfig, playerData.copy(uid = "player2")),
      Player[IO](playerConfig, playerData.copy(uid = "player3"))
    )

    players.foreach(p => repository.update(p).unsafeRunSync())

    val result = repository.getAll.unsafeRunSync()
    assertEquals(result, players)
  }

  test("update should update the player in the repository") {
    val updatedColor = "red"
    val playerToBeUpdated: Player[IO] = player.copy(playerData = player.playerData.copy(color = updatedColor))
    val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
    repository.update(playerToBeUpdated).unsafeRunSync()
    val result = repository.get(player.playerData.uid).unsafeRunSync()
    assertEquals(result, Some(playerToBeUpdated))
  }

  test("delete should remove the player from the repository") {
    val repository = PlayerRepository.inMemory[IO].unsafeRunSync()
    repository.update(player).unsafeRunSync()
    repository.delete(player.playerData.uid).unsafeRunSync()
    val result = repository.get(player.playerData.uid).unsafeRunSync()
    assertEquals(result, None)
  }
}
