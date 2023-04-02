package routes

import cats.*
import org.http4s.*
import org.http4s.dsl.*

final class HealthCheckRoutes[F[_]: Monad] extends Http4sDsl[F]:
  def routes: HttpRoutes[F] =
    HttpRoutes.of[F] { case GET -> Root / "health" => Ok("Ok - agarwinner.io") }
