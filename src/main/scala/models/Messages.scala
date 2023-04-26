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

enum Request derives ConfiguredEncoder, ConfiguredDecoder:
  case InitMessage(data: InitData)
  case TickMessage(data: TickData)

object Request:
  given Configuration = Configuration.default.withDiscriminator("_type")

case class InitData(playerName: String)
case class TickData(uid: String, xVector: Double, yVector: Double)

enum Response derives ConfiguredEncoder, ConfiguredDecoder:
  case InitMessageResponse(
      messageType: String,
      data: InitMessageResponseData
  )
  case PlayerListMessageResponse(
      messageType: String,
      data: Vector[PlayerData]
  )

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
