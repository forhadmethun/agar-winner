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

enum Request derives ConfiguredDecoder:
  case InitMessage(data: InitData)
  case TickMessage(data: TickData)

object Request:
  given Configuration = Configuration.default.withDiscriminator("_type")

case class InitData(playerName: String, sid: String)
case class TickData(uid: String, xVector: Double, yVector: Double)

enum Response derives ConfiguredEncoder:
  case InitMessageResponse(data: InitMessageResponseData)
  case PlayerListMessageResponse(data: Vector[PlayerData])
  case TickMessageResponse(data: TickMessageResponseData)

object Response:
  given Configuration = Configuration.default.withDiscriminator("_type")

final case class InitMessageResponseData(orbs: Vector[OrbData], playerData: PlayerData)
final case class TickMessageResponseData(playerData: PlayerData, orbs: Vector[OrbData])

object InitMessageResponseData {
  def create[F[_] : Sync](orbs: Vector[OrbData], playerData: PlayerData): F[InitMessageResponseData] =
    Sync[F].pure(InitMessageResponseData(orbs, playerData))
}
