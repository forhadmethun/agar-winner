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

object CollisionChecker:
  def isOrbCollidingWithPlayer(pData: PlayerData, collider: CircularShape): Boolean =
    isAABBTestPassing(pData, collider) && isPythagorasTestPassing(pData, collider)

  def isAABBTestPassing(pData: PlayerData, collider: CircularShape): Boolean =
    pData.locX + pData.radius + collider.radius > collider.locX &&
      pData.locX < collider.locX + pData.radius + collider.radius &&
      pData.locY + pData.radius + collider.radius > collider.locY &&
      pData.locY < collider.locY + pData.radius + collider.radius

  def isPythagorasTestPassing(pData: PlayerData, collider: CircularShape): Boolean =
    val distanceSquared = math.pow(pData.locX - collider.locX, 2) +
      math.pow(pData.locY - collider.locY, 2)
    val radiusSumSquared = math.pow(pData.radius + collider.radius, 2)
    distanceSquared < radiusSumSquared
