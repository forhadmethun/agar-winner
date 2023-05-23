package core

import cats.*
import cats.effect.*
import cats.implicits.*
import models.*
import models.OrbData.*
import models.PlayerData.*
import models.PlayerConfig.*
import models.Settings.*
import services.*

trait CollisionProcessor[F[_]]:
  def checkAndProcessOrbCollision(player: Player[F], orbs: Vector[Orb[F]]): F[Unit]
  def checkAndProcessPlayerCollisions(player: Player[F], players: Vector[Player[F]]): F[Unit]
  def handleCollisionOrb(collisionOrb: OrbData, player: Player[F]): F[Unit]
  def handleCollisionPlayer(player: Player[F], collisionPlayer: Player[F]): F[Unit]

object CollisionProcessor:
  def create[F[_]: Sync](
    playerService: PlayerService[F],
    orbService: OrbService[F]
  ): CollisionProcessor[F] = new CollisionProcessor[F] {

    def checkAndProcessOrbCollision(player: Player[F], orbs: Vector[Orb[F]]): F[Unit] =
      orbs.map(_.orbData)
        .find(isOrbCollidingWithPlayer(player.playerData, _))
        .fold(Sync[F].unit)(handleCollisionOrb(_, player))

    def handleCollisionOrb(collisionOrb: OrbData, player: Player[F]): F[Unit] = {
      for
        updatedOrb <- createUpdatedOrb(collisionOrb)
        _ <- orbService.saveOrb(updatedOrb)
        _ <- playerService.savePlayer(
          updatePlayerConfig(player.playerConfig),
          updatePlayerData(player.playerData)
        )
      yield ()
    }

    def checkAndProcessPlayerCollisions(player: Player[F], players: Vector[Player[F]]): F[Unit] =
      players.filter(_.playerData.uid != player.playerData.uid)
        .find(p => isOrbCollidingWithPlayer(player.playerData, p.playerData))
        .fold(Sync[F].unit)(handleCollisionPlayer(player, _))

    def handleCollisionPlayer(player: Player[F], collisionPlayer: Player[F]): F[Unit] = {
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

  def calculateNewLocation(playerData: PlayerData, tickData: TickData, speed: Double): (Double, Double) = {
    val currentLocX = playerData.locX
    val currentLocY = playerData.locY
    val xVector = tickData.xVector
    val yVector = tickData.yVector

    val isAtLeftBoundary = currentLocX < 0 && xVector < 0
    val isAtRightBoundary = currentLocX > worldWidth && xVector > 0
    val isAtTopBoundary = currentLocY < 0 && yVector > 0
    val isAtBottomBoundary = currentLocY > worldHeight && yVector < 0

    val newLocX = if isAtLeftBoundary || isAtRightBoundary then {
      currentLocX
    } else {
      currentLocX + speed * xVector
    }

    val newLocY = if isAtTopBoundary || isAtBottomBoundary then {
      currentLocY
    } else {
      currentLocY - speed * yVector
    }

    (newLocX, newLocY)
  }

  def isOrbCollidingWithPlayer(pData: PlayerData, collider: CircularShape): Boolean = {
    isAABBTestPassing(pData, collider) && isPythagorasTestPassing(pData, collider)
  }

  def isAABBTestPassing(pData: PlayerData, collider: CircularShape): Boolean = {
    pData.locX + pData.radius + collider.radius > collider.locX &&
      pData.locX < collider.locX + pData.radius + collider.radius &&
      pData.locY + pData.radius + collider.radius > collider.locY &&
      pData.locY < collider.locY + pData.radius + collider.radius
  }

  def isPythagorasTestPassing(pData: PlayerData, collider: CircularShape): Boolean = {
    val distanceSquared = math.pow(pData.locX - collider.locX, 2) +
      math.pow(pData.locY - collider.locY, 2)
    val radiusSumSquared = math.pow(pData.radius + collider.radius, 2)
    distanceSquared < radiusSumSquared
  }
