package services

import cats.effect.IO
import models.OrbData
import cats.effect.unsafe.implicits.global
import munit.FunSuite
import repository.PlayerRepository
import models.*
import org.scalacheck.Gen
import org.scalacheck.Prop.forAll
class PlayerServiceTest extends FunSuite {
  val playerConfigGen: Gen[PlayerConfig] = for {
    speed <- Gen.posNum[Double]
    zoom <- Gen.posNum[Double]
  } yield PlayerConfig(speed = speed, zoom = zoom)

  val playerDataGen: Gen[PlayerData] = for {
    playerId <- Gen.alphaNumStr
    playerName <- Gen.alphaNumStr
    sid <- Gen.alphaNumStr
    locX <- Gen.posNum[Double]
    locY <- Gen.posNum[Double]
    color <- Gen.alphaStr
    radius <- Gen.posNum[Double]
  } yield PlayerData(playerId, sid, playerName, locX, locY, color, radius)

  test("getAllPlayers should return all players") {
    val getAllPlayersProp = forAll(Gen.listOfN(5, Gen.zip(playerConfigGen, playerDataGen))) { playersList =>
      val players = playersList.map { case (playerConfig, playerData) => Player[IO](playerConfig, playerData) }
      val repo = new PlayerRepository[IO] {
        override def getAll: IO[Vector[Player[IO]]] = IO.pure(players.toVector)
        override def get(uid: String): IO[Option[Player[IO]]] = ???
        override def update(player: Player[IO]): IO[Unit] = ???
        override def delete(uid: String): IO[Unit] = ???
      }
      val service = new PlayerService[IO](repo)
      val result = service.getAllPlayers.unsafeRunSync()
      result == players.toVector
    }
    getAllPlayersProp.check()
  }

  test("getPlayer should return a player with the given ID") {
    val getPlayerProp = forAll(playerConfigGen, playerDataGen) { (playerConfig, playerData) =>
      val player = Player[IO](playerConfig, playerData)
      val repo = new PlayerRepository[IO] {
        override def get(uid: String): IO[Option[Player[IO]]] =
          if (uid == playerData.uid) IO.pure(Some(player)) else IO.pure(None)
        override def getAll: IO[Vector[Player[IO]]] = ???
        override def update(player: Player[IO]): IO[Unit] = ???
        override def delete(uid: String): IO[Unit] = ???
      }

      val service = new PlayerService[IO](repo)
      val result = service.getPlayer(playerData.uid).unsafeRunSync()
      result == player
    }
    getPlayerProp.check()
  }

  test("getPlayer should throw an exception when no player is found with the given ID") {
    val playerIdGen: Gen[String] = Gen.alphaNumStr
    val getPlayerProp = forAll(playerIdGen) { playerId =>
      val repo = new PlayerRepository[IO] {
        override def get(uid: String): IO[Option[Player[IO]]] = IO.pure(None)
        override def getAll: IO[Vector[Player[IO]]] = ???
        override def update(player: Player[IO]): IO[Unit] = ???
        override def delete(uid: String): IO[Unit] = ???
      }
      val service = new PlayerService(repo)
      val result = service.getPlayer(playerId).attempt.unsafeRunSync()
      result.isLeft && result.isInstanceOf[Left[Throwable, _]] &&
        result.swap.toOption.get.isInstanceOf[IllegalArgumentException] &&
        result.swap.toOption.get.getMessage == s"No player found with id: $playerId"
    }
    getPlayerProp.check()
  }

  test("savePlayer should return the saved player") {
    val savePlayerProp = forAll(playerConfigGen, playerDataGen) { (playerConfig, playerData) =>
      val repo = new PlayerRepository[IO] {
        override def update(player: Player[IO]): IO[Unit] = IO.unit
        override def get(uid: String): IO[Option[Player[IO]]] = ???
        override def getAll: IO[Vector[Player[IO]]] = ???
        override def delete(uid: String): IO[Unit] = ???
      }
      val service = new PlayerService(repo)
      val result = service.savePlayer(playerConfig, playerData).unsafeRunSync()
      result.playerConfig == playerConfig && result.playerData == playerData
    }
    savePlayerProp.check()
  }

  test("deletePlayer should delete the player") {
    val playerIdGen: Gen[String] = Gen.alphaNumStr
    val playerIdProp = forAll(playerIdGen) { playerId =>
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
      deletedPlayerId.contains(playerId)
    }
    playerIdProp.check()
  }

  test("create should return a PlayerService instance") {
    val result = PlayerService.create[IO].unsafeRunSync()
    assert(result.isInstanceOf[PlayerService[IO]])
  }
}
