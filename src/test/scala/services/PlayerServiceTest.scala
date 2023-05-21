package services

import cats.effect.IO
import models.OrbData
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import repository.PlayerRepository
import models.*

class PlayerServiceTest extends FunSuite {
  test("getAllPlayers should return all players") {
    val playerConfig = PlayerConfig(speed = 0, zoom = 0.0)
    val playerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
    val player1 = Player[IO](playerConfig, playerData)
    val repo = new PlayerRepository[IO] {
      override def getAll: IO[Vector[Player[IO]]] = IO.pure(Vector(player1));
      override def get(uid: String): IO[Option[Player[IO]]] = ???
      override def update(player: Player[IO]): IO[Unit] = ???
      override def delete(uid: String): IO[Unit] = ???
    }
    val service = new PlayerService[IO](repo)
    val result = service.getAllPlayers.unsafeRunSync()
    val expectedPlayers = Vector(player1);
    assertEquals(result, expectedPlayers)
  }

  test("getPlayer should return a player with the given ID") {
    val playerConfig = PlayerConfig(speed = 0, zoom = 0.0)
    val playerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
    val player = Player[IO](playerConfig, playerData)
    val repo = new PlayerRepository[IO] {
      override def get(uid: String): IO[Option[Player[IO]]] =
        if (uid == playerData.uid) IO.pure(Some(player))
        else IO.pure(None)
      override def getAll: IO[Vector[Player[IO]]] = ???
      override def update(player: Player[IO]): IO[Unit] = ???
      override def delete(uid: String): IO[Unit] = ???
    }

    val service = new PlayerService[IO](repo)
    val result = service.getPlayer(playerData.uid).unsafeRunSync()
    assertEquals(result, player)
  }

  test("getPlayer should throw an exception when no player is found with the given ID") {
    val playerId = "123"

    val repo = new PlayerRepository[IO] {
      override def get(uid: String): IO[Option[Player[IO]]] = IO.pure(None)
      override def getAll: IO[Vector[Player[IO]]] = ???
      override def update(player: Player[IO]): IO[Unit] = ???
      override def delete(uid: String): IO[Unit] = ???
    }

    val service = new PlayerService(repo)
    val result = service.getPlayer(playerId).attempt.unsafeRunSync()
    assert(result.isLeft)
    assert(result.isInstanceOf[Left[Throwable, _]])
    assert(result.swap.toOption.get.isInstanceOf[IllegalArgumentException])
    assertEquals(result.swap.toOption.get.getMessage, s"No player found with id: $playerId")
  }

  test("savePlayer should return the saved player") {
    val playerConfig = PlayerConfig(speed = 0, zoom = 0.0)
    val playerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
    val repo = new PlayerRepository[IO] {
      override def update(player: Player[IO]): IO[Unit] = IO.unit
      override def get(uid: String): IO[Option[Player[IO]]] = ???
      override def getAll: IO[Vector[Player[IO]]] = ???
      override def delete(uid: String): IO[Unit] = ???
    }
    val service = new PlayerService(repo)
    val result = service.savePlayer(playerConfig, playerData).unsafeRunSync()
    assertEquals(result.playerConfig, playerConfig)
    assertEquals(result.playerData, playerData)
  }

  test("deletePlayer should delete the player") {
    val playerId = "123"
    var deletedPlayerId: Option[String] = None
    val repo = new PlayerRepository[IO] {
      override def delete(uid: String): IO[Unit] = {
        deletedPlayerId = Some(uid)
        IO.unit
      }
      override def get(uid: String): IO[Option[Player[IO]]] = ???
      override def getAll: IO[Vector[Player[IO]]] = ???
      override def update(player: Player[IO]): IO[Unit] = ???
    }
    val service = new PlayerService(repo)
    service.deletePlayer(playerId).unsafeRunSync()
    assertEquals(deletedPlayerId, Some(playerId))
  }

  test("create should return a PlayerService instance") {
    val result = PlayerService.create[IO].unsafeRunSync()
    assert(result.isInstanceOf[PlayerService[IO]])
  }
}
