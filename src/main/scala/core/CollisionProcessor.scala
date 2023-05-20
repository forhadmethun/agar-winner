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
  def checkAndProcessOrbCollision(uid: String): F[Unit]
  def checkAndProcessPlayerCollisions(uid: String): F[Unit]
  def handleCollisionOrb(collisionOrbOpt: Option[OrbData], player: Player[F]): F[Option[Unit]]
  def handleCollisionPlayer(collisionPlayerOpt: Option[Player[F]], player: Player[F]): F[Option[Unit]]

object CollisionProcessor:
  def create[F[_]: Sync](
                           playerService: PlayerService[F],
                           orbService: OrbService[F]
                         ): CollisionProcessor[F] = new CollisionProcessor[F] {
    def checkAndProcessOrbCollision(uid: String): F[Unit] = {
      for {
        player <- playerService.getPlayer(uid)
        orbs <- orbService.getAllOrbs
        collisionOrbOpt = orbs.map(_.orbData)
          .find(orb => isOrbCollidingWithPlayer(player.playerData, orb))
        _ <- handleCollisionOrb(collisionOrbOpt, player)
      } yield ()
    }

    def checkAndProcessPlayerCollisions(uid: String): F[Unit] = {
      for {
        player <- playerService.getPlayer(uid)
        players <- playerService.getAllPlayers
        collisionPlayerOpt = players.filter(_.playerData.uid != uid)
          .find(p => isOrbCollidingWithPlayer(player.playerData, p.playerData))
        _ <- handleCollisionPlayer(collisionPlayerOpt, player)
      } yield ()
    }

    def handleCollisionOrb(collisionOrbOpt: Option[OrbData], player: Player[F]): F[Option[Unit]] = {
      collisionOrbOpt match {
        case Some(collisionOrb) =>
          val updatedPlayerData = updatePlayerData(player.playerData)
          val updatedPConfig = updatePlayerConfig(player.playerConfig)
          for {
            updatedOrb <- createUpdatedOrb(collisionOrb)
            _ <- orbService.saveOrb(updatedOrb)
            _ <- playerService.savePlayer(updatedPConfig, updatedPlayerData)
          } yield Some(())
        case None => Sync[F].pure(None)
      }
    }

    def handleCollisionPlayer(collisionPlayerOpt: Option[Player[F]], player: Player[F]): F[Option[Unit]] = {
      collisionPlayerOpt match {
        case Some(collisionPlayer) =>
          val (playerToBeUpdated, playerToBeDeleted) =
            if (player.playerData.radius > collisionPlayer.playerData.radius)
              (player, collisionPlayer)
            else
              (collisionPlayer, player)
          val updatedPlayerData = updatePlayerData(playerToBeUpdated.playerData)
          val updatedPlayerConfig = updatePlayerConfig(playerToBeUpdated.playerConfig)
          for {
            _ <- playerService.deletePlayer(playerToBeDeleted.playerData.uid)
            _ <- playerService.savePlayer(updatedPlayerConfig, updatedPlayerData)
          } yield Some(())
        case _ => Sync[F].pure(None)
      }
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

    val newLocX = if (isAtLeftBoundary || isAtRightBoundary) {
      currentLocX
    } else {
      currentLocX + speed * xVector
    }

    val newLocY = if (isAtTopBoundary || isAtBottomBoundary) {
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
