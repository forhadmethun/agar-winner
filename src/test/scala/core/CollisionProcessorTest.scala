package core

import cats.effect.IO
import cats.syntax.all.*
import core.CollisionProcessor.*
import cats.effect.unsafe.implicits.global
import models.*
import munit.FunSuite
import services.{OrbService, PlayerService}

class CollisionProcessorTest extends FunSuite {
  val playerConfig: PlayerConfig = PlayerConfig(speed = 0, zoom = 0.0)
  val playerData: PlayerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
  val player: Player[IO] = Player[IO](playerConfig, playerData)

  test("calculateNewLocation should calculate new location correctly") {
    val playerData = PlayerData(uid = "player1", sid = "sid1", playerName = "Player 1", locX = 10, locY = 10, color = "red", radius = 5)
    val tickData = TickData(uid = "player1", xVector = 1, yVector = -1)
    val speed = 0.1

    val (newLocX, newLocY) = calculateNewLocation(playerData, tickData, speed)

    assertEquals(newLocX, 10.1)
    assertEquals(newLocY, 10.1)
  }

  test(
    "isOrbCollidingWithPlayer should return true when orb collides with player"
  ) {
    val pData = PlayerData(
      uid = "player1",
      sid = "sid1",
      playerName = "John",
      locX = 10.0,
      locY = 10.0,
      color = "blue",
      radius = 5.0
    )
    val orb = OrbData("orb1", "red", 15, 15, 3)

    val result = isOrbCollidingWithPlayer(pData, orb)

    assertEquals(result, true)
  }

  test(
    "isOrbCollidingWithPlayer should return false when orb does not collide with player"
  ) {
    val pData = PlayerData(
      uid = "player1",
      sid = "sid1",
      playerName = "John",
      locX = 10.0,
      locY = 10.0,
      color = "blue",
      radius = 5
    )
    val orb =
      OrbData(uid = "orb1", color = "red", locX = 25, locY = 25, radius = 3)

    val result = isOrbCollidingWithPlayer(pData, orb)

    assertEquals(result, false)
  }

  test("isAABBTestPassing should return true when AABB test passes") {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "red", 15, 15, 3)

    val result = isAABBTestPassing(pData, orb)

    assertEquals(result, true)
  }

  test("isAABBTestPassing should return false when AABB test fails") {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "green", 25, 25, 3)

    val result = isAABBTestPassing(pData, orb)

    assertEquals(result, false)
  }

  test(
    "isPythagorasTestPassing should return true when Pythagoras test passes"
  ) {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "red", 13, 13, 3)

    val result = isPythagorasTestPassing(pData, orb)

    assertEquals(result, true)
  }

  test(
    "isPythagorasTestPassing should return false when Pythagoras test fails"
  ) {
    val pData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 5.0)
    val orb = OrbData("orb1", "green", 25, 25, 3)

    val result = isPythagorasTestPassing(pData, orb)

    assertEquals(result, false)
  }

  test("handleCollisionPlayer should delete the collision player and update the current player") {
    // Create the test players
    val player1: Player[IO] = player.copy(playerData = player.playerData.copy(playerName = "player1", radius = 5.0))
    val player2: Player[IO] = player.copy(playerData = player.playerData.copy(playerName = "player2", radius = 3.0))

    // Create an in-memory PlayerRepository
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val playerRepo = playerService.repo


    // Add the players to the repository
    playerRepo.update(player1).unsafeRunSync()
    playerRepo.update(player2).unsafeRunSync()

    for {
      allPlayers <- playerService.getAllPlayers
      _ <- IO(assert(allPlayers.length == 2))
    } yield ()

    // Call the handleCollisionPlayer method
    val result = collisionProcessor.handleCollisionPlayer(player1, player2)

    // Assert the result
    result.flatMap { _ =>
      for {
        allPlayers <- playerService.getAllPlayers
        _ <- IO(assert(allPlayers.length == 1))
        updatedPlayer <- playerService.getPlayer(player1.playerData.uid)
        _ <- IO(assert(updatedPlayer.playerData.radius > player1.playerData.radius))
      } yield ()
    }.unsafeRunSync()
  }

  test("checkAndProcessPlayerCollisions should handle collision when player collides with another player") {
    // Create the test players
    val player1: Player[IO] = player.copy(playerData = player.playerData.copy(playerName = "player1", radius = 5.0))
    val player2: Player[IO] = player.copy(playerData = player.playerData.copy(playerName = "player2", radius = 3.0))

    // Create an in-memory PlayerRepository
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val playerRepo = playerService.repo


    // Add the players to the repository
    playerRepo.update(player1).unsafeRunSync()
    playerRepo.update(player2).unsafeRunSync()

    for {
      allPlayers <- playerService.getAllPlayers
      _ <- IO(assert(allPlayers.length == 2))
    } yield ()

    // Call the handleCollisionPlayer method
    val result = collisionProcessor.checkAndProcessPlayerCollisions(player1, playerRepo.getAll.unsafeRunSync())

    // Assert the result
    result.flatMap { _ =>
      for {
        allPlayers <- playerService.getAllPlayers
        _ <- IO(assert(allPlayers.length == 1))
      } yield ()
    }.unsafeRunSync()
  }

  test("handleCollisionOrb should save the updated orb and update the player") {
    // Create the test player and orb
    val orb = Orb[IO](OrbData("orb1", "red", 15, 15, 3))

    // Create an in-memory PlayerRepository & OrbService
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val playerRepo = playerService.repo


    // Add the player to the repository
    playerRepo.update(player).unsafeRunSync()

    // Call the handleCollisionOrb method
    val result = collisionProcessor.handleCollisionOrb(orb.orbData, player)

    // Assert the result
    result.flatMap { _ =>
      for {
        updatedOrbOpt <- orbService.getOrb(orb.orbData.uid)
        _ <- IO(assert(updatedOrbOpt != null))
        updatedPlayer <- playerService.getPlayer(player.playerData.uid)
        _ <- IO(assert(updatedPlayer.playerData.radius > player.playerData.radius))
      } yield ()
    }.unsafeRunSync()
  }

  test("checkAndProcessOrbCollision should handle collision when orb collides with player") {
    // Create the test player and orb
    val orb = Orb[IO](OrbData("orb1", "red", 15, 15, 3))

    // Create an in-memory PlayerRepository & OrbService
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val playerRepo = playerService.repo


    // Add the player to the repository
    playerRepo.update(player).unsafeRunSync()

    // Add orb to the repository
    orbService.saveOrb(orb.orbData).unsafeRunSync()

    // Call the handleCollisionOrb method
    val result = collisionProcessor.checkAndProcessOrbCollision(player, orbService.getAllOrbs.unsafeRunSync())

    // Assert the result
    result.flatMap { _ =>
      for {
        updatedOrbOpt <- orbService.getOrb(orb.orbData.uid)
        _ <- IO(assert(updatedOrbOpt != null))
        updatedPlayer <- playerService.getPlayer(player.playerData.uid)
        _ <- IO(assert(updatedPlayer.playerData.radius > player.playerData.radius))
      } yield ()
    }.unsafeRunSync()
  }
}
