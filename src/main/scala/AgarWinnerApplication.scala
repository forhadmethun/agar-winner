import cats.*
import cats.effect.*
import cats.effect.std.Queue
import cats.syntax.all.*
import com.comcast.ip4s.{Host, Port}
import fs2.concurrent.Topic
import io.circe.*
import io.circe.syntax.*
import org.http4s.*
import org.http4s.circe.*
import org.http4s.dsl.*
import org.http4s.dsl.impl.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import org.http4s.server.*
import routes.{GameRoutes, HealthCheckRoutes, HomeRoutes}

object AgarWinnerApplication extends IOApp.Simple with Http4sDsl[IO]:
  def run =
    for
      gameRoutes <- GameRoutes.setup[IO]
      _ <- EmberServerBuilder
        .default[IO]
        .withHostOption(Host.fromString("0.0.0.0"))
        .withPort(Port.fromInt(8090).get)
        .withHttpWebSocketApp { wsb =>
          HomeRoutes[IO].routes
            .combineK(HealthCheckRoutes[IO].routes)
            .combineK(gameRoutes.wsRoutes(wsb))
            .orNotFound
        }
        .withErrorHandler { e =>
          IO.println("Could not handle a request" -> e) *> InternalServerError()
        }
        .build
        .use(_ => IO.never)
        .void
    yield ()
