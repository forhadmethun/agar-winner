package models

import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax.EncoderOps

object Messages {
  final case class InitMessageResponseData(orbs: List[Orb])
      derives Codec.AsObject
  final case class InitMessageResponse(
      `type`: String,
      data: InitMessageResponseData
  ) derives Codec.AsObject
  case class InitMessage(`type`: String, data: InitData)
  case class InitData(playerName: String)
  case class TickMessage(`type`: String, data: TickData)
  case class TickData(xVector: Double, yVector: Double)
}
