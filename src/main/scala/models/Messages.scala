package models

import cats.effect.Sync
import cats.syntax.all.*
import io.circe.*
import io.circe.derivation.{
  Configuration,
  ConfiguredDecoder,
  ConfiguredEncoder,
  ConfiguredEnumEncoder
}
import io.circe.syntax.EncoderOps

final case class GameMessage(content: String)

enum Request derives ConfiguredDecoder:
  case InitMessage(data: InitData)
  case TickMessage(data: TickData)

object Request:
  given Configuration = Configuration.default.withDiscriminator("_type")

case class InitData(playerName: String, sid: String)
case class TickData(uid: String, xVector: Double, yVector: Double)

enum Response derives ConfiguredEncoder, ConfiguredDecoder:
  case InitMessageResponse(data: InitMessageResponseData)
  case PlayerListMessageResponse(data: Vector[PlayerData])
  case TickMessageResponse(data: TickMessageResponseData)

object Response:
  given Configuration = Configuration.default.withDiscriminator("_type")

final case class InitMessageResponseData(orbs: Vector[OrbData], playerData: PlayerData)
final case class TickMessageResponseData(playerData: PlayerData, orbs: Vector[OrbData])
