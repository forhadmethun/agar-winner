package core

import cats.MonadThrow
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import cats.syntax.all.*
import io.circe.parser.*
import io.circe.syntax.*
import core.CollisionProcessor.*
import models.*
import munit.FunSuite
import services.{OrbService, PlayerService}

class GameMessageProcessorTest extends FunSuite {
  val playerConfig: PlayerConfig = PlayerConfig(speed = 0, zoom = 0.0)
  val playerData: PlayerData = PlayerData("player1", "sid1", "John", 10.0, 10.0, "blue", 10.0)
  val player: Player = Player(playerConfig, playerData)

  test("processInitMessage should return the expected GameMessage") {
    val initMsg: Request.InitMessage = Request.InitMessage(InitData(playerData.playerName, playerData.sid))
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create(playerService, orbService)
    val gameMessageProcessor = GameMessageProcessor.create(playerService, orbService, collisionProcessor)

    val msg = for
      msg <- gameMessageProcessor.processInitMessage(initMsg)
      parsedMsg <- MonadThrow[IO].fromEither(decode[Response](msg.content))
    yield parsedMsg
    val initMsgResponse = msg.unsafeRunSync()
    initMsgResponse match 
      case msg: Response.InitMessageResponse =>
        assertEquals(msg.data.playerData.sid, playerData.sid)
        assertEquals(msg.data.playerData.playerName, playerData.playerName)
      case _ =>
  }

  test("processTickMessage should return the expected GameMessage") {
    val initMsg: Request.InitMessage = Request.InitMessage(InitData("player1", "id1"))
    val playerService = PlayerService.create[IO].unsafeRunSync()
    val orbService = OrbService.create[IO].unsafeRunSync()
    val collisionProcessor = CollisionProcessor.create[IO](playerService, orbService)
    val gameMessageProcessor = GameMessageProcessor.create[IO](playerService, orbService, collisionProcessor)
    val initMsgResponse = for
      msg <- gameMessageProcessor.processInitMessage(initMsg)
      parsedMsg <- MonadThrow[IO].fromEither(decode[Response](msg.content))
    yield parsedMsg
    val initResponse = initMsgResponse.unsafeRunSync()
    initResponse match {
      case initRes: Response.InitMessageResponse =>
        val tickMessage: Request.TickMessage = Request.TickMessage(TickData(initRes.data.playerData.uid, 0.022d, 0.977d))
        val tickMsgResponse = for
          tickMsg <- gameMessageProcessor.processTickMessage(tickMessage)
          parsedMsg <- MonadThrow[IO].fromEither(decode[Response](tickMsg.content))
        yield parsedMsg
        val tickResponse = tickMsgResponse.unsafeRunSync()
        tickResponse match
          case msg: Response.TickMessageResponse =>
            assertEquals(initRes.data.playerData.uid, initRes.data.playerData.uid)
            assertEquals(initRes.data.playerData.sid, initRes.data.playerData.sid)
            assertEquals(initRes.data.playerData.playerName, initRes.data.playerData.playerName)
            assert(msg.data.playerData.locX != initRes.data.playerData.locX)
            assert(msg.data.playerData.locY != initRes.data.playerData.locY)
          case _ =>
      case _ =>
    }
  }
}
