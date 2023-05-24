package core

import cats.*
import cats.effect.*
import cats.implicits.*
import core.CollisionChecker.*
import models.*
import models.OrbData.*
import models.PlayerData.*
import models.PlayerConfig.*
import models.Settings.*
import services.*
trait CollisionProcessor[F[_]]:
  def checkAndProcessOrbCollision(player: Player, orbs: Vector[Orb]): F[Unit]
  def checkAndProcessPlayerCollisions(player: Player, players: Vector[Player]): F[Unit]
  def handleCollisionOrb(collisionOrb: OrbData, player: Player): F[Unit]
  def handleCollisionPlayer(player: Player, collisionPlayer: Player): F[Unit]

object CollisionProcessor:
  def create[F[_]: Sync](
    playerService: PlayerService[F],
    orbService: OrbService[F]
  ): CollisionProcessor[F] = new CollisionProcessor[F] {

    def checkAndProcessOrbCollision(player: Player, orbs: Vector[Orb]): F[Unit] =
      orbs.map(_.orbData)
        .find(isOrbCollidingWithPlayer(player.playerData, _))
        .fold(Sync[F].unit)(handleCollisionOrb(_, player))

    def handleCollisionOrb(collisionOrb: OrbData, player: Player): F[Unit] = {
      for
        updatedOrb <- createUpdatedOrb(collisionOrb)
        _ <- orbService.saveOrb(updatedOrb)
        _ <- playerService.savePlayer(
          updatePlayerConfig(player.playerConfig),
          updatePlayerData(player.playerData)
        )
      yield ()
    }

    def checkAndProcessPlayerCollisions(player: Player, players: Vector[Player]): F[Unit] =
      players.filter(_.playerData.uid != player.playerData.uid)
        .find(p => isOrbCollidingWithPlayer(player.playerData, p.playerData))
        .fold(Sync[F].unit)(handleCollisionPlayer(player, _))

    def handleCollisionPlayer(player: Player, collisionPlayer: Player): F[Unit] = {
      val (playerToBeUpdated, playerToBeDeleted) = Player.getPlayerToBeUpdatedAndDeleted(player, collisionPlayer)
      for
        _ <- playerService.deletePlayer(playerToBeDeleted.playerData.uid)
        _ <- playerService.savePlayer(
          updatePlayerConfig(playerToBeUpdated.playerConfig),
          updatePlayerData(playerToBeUpdated.playerData))
        orbs <- Orb.generateNearestOrbs[F](playerToBeDeleted.playerData.locX, playerToBeDeleted.playerData.locY,
          Math.sqrt(playerToBeDeleted.playerData.score).toInt)
        _ <- orbService.saveAllOrb(orbs)
      yield ()
    }
  }
