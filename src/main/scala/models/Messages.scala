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

case class InitData(playerName: String)
case class TickData(uid: String, xVector: Double, yVector: Double)

enum Response derives ConfiguredEncoder:
  case InitMessageResponse(data: InitMessageResponseData)
  case PlayerListMessageResponse(data: Vector[PlayerData])
  case TickMessageResponse(data: PlayerData)

object Response:
  given Configuration = Configuration.default.withDiscriminator("_type")

final case class InitMessageResponseData(
    orbs: Vector[Orb],
    playerData: PlayerData
)

object InitMessageResponseData {
  def create[F[_]: Sync](playerData: PlayerData): F[InitMessageResponseData] = {
    Orb.generateOrbs[F].map(orbs => InitMessageResponseData(orbs, playerData))
  }
}
