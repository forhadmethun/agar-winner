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
  val player: Player = Player(playerConfig, playerData)

  test("handleCollisionPlayer should delete the collision player and update the current player") {
    // Create the test players
    val player1: Player = player.copy(playerData = player.playerData.copy(playerName = "player1", radius = 5.0))
    val player2: Player = player.copy(playerData = player.playerData.copy(playerName = "player2", radius = 3.0))

    // Create an in-memory PlayerRepository
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val playerRepo = playerService.repo


    // Add the players to the repository
    playerRepo.update(player1).unsafeRunSync()
    playerRepo.update(player2).unsafeRunSync()

    for
      allPlayers <- playerService.getAllPlayers
      _ <- IO(assert(allPlayers.length == 2))
    yield ()

    // Call the handleCollisionPlayer method
    val result = collisionProcessor.handleCollisionPlayer(player1, player2)

    // Assert the result
    result.flatMap { _ =>
      for
        allPlayers <- playerService.getAllPlayers
        _ <- IO(assert(allPlayers.length == 1))
        updatedPlayer <- playerService.getPlayer(player1.playerData.uid)
        _ <- IO(assert(updatedPlayer.playerData.radius > player1.playerData.radius))
      yield ()
    }.unsafeRunSync()
  }

  test("checkAndProcessPlayerCollisions should handle collision when player collides with another player") {
    // Create the test players
    val player1: Player = player.copy(playerData = player.playerData.copy(playerName = "player1", radius = 5.0))
    val player2: Player = player.copy(playerData = player.playerData.copy(playerName = "player2", radius = 3.0))

    // Create an in-memory PlayerRepository
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val playerRepo = playerService.repo


    // Add the players to the repository
    playerRepo.update(player1).unsafeRunSync()
    playerRepo.update(player2).unsafeRunSync()

    for
      allPlayers <- playerService.getAllPlayers
      _ <- IO(assert(allPlayers.length == 2))
    yield ()

    // Call the handleCollisionPlayer method
    val result = collisionProcessor.checkAndProcessPlayerCollisions(player1, playerRepo.getAll.unsafeRunSync())

    // Assert the result
    result.flatMap { _ =>
      for
        allPlayers <- playerService.getAllPlayers
        _ <- IO(assert(allPlayers.length == 1))
      yield ()
    }.unsafeRunSync()
  }

  test("handleCollisionOrb should save the updated orb and update the player") {
    // Create the test player and orb
    val orb = Orb(OrbData("orb1", "red", 15, 15, 3))

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
      for
        updatedOrbOpt <- orbService.getOrb(orb.orbData.uid)
        _ <- IO(assert(updatedOrbOpt != null))
        updatedPlayer <- playerService.getPlayer(player.playerData.uid)
        _ <- IO(assert(updatedPlayer.playerData.radius > player.playerData.radius))
      yield ()
    }.unsafeRunSync()
  }

  test("checkAndProcessOrbCollision should handle collision when orb collides with player") {
    // Create the test player and orb
    val orb = Orb(OrbData("orb1", "red", 15, 15, 3))

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
      for
        updatedOrbOpt <- orbService.getOrb(orb.orbData.uid)
        _ <- IO(assert(updatedOrbOpt != null))
        updatedPlayer <- playerService.getPlayer(player.playerData.uid)
        _ <- IO(assert(updatedPlayer.playerData.radius > player.playerData.radius))
      yield ()
    }.unsafeRunSync()
  }
}
