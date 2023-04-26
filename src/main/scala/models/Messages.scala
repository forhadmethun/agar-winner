package models

import cats.effect.Sync
import cats.syntax.all._
import io.circe._
import io.circe.syntax.EncoderOps

sealed trait Response derives Codec.AsObject
final case class InitMessageResponse(
    messageType: String,
    data: InitMessageResponseData
) extends Response
final case class InitMessageResponseData(orbs: Vector[Orb])

object InitMessageResponseData {
  def create[F[_]: Sync]: F[InitMessageResponseData] = {
    Orb.generateOrbs[F].map(orbs => InitMessageResponseData(orbs))
  }
}

sealed trait Request
final case class InitMessage(messageType: String, data: InitData)
    extends Request
case class TickMessage(messageType: String, data: TickData) extends Request
case class InitData(playerName: String)
case class TickData(xVector: Double, yVector: Double)
