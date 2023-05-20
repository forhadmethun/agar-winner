package routes

import cats.*
import cats.effect.*
import cats.implicits.*
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.implicits.*

  final class HomeRoutes[F[_] : Sync] extends Http4sDsl[F] :
    def static(file: String, request: Request[F])(implicit F: Sync[F]): F[Response[F]] =
      StaticFile.fromResource("/static/" + file, Some(request)).getOrElseF(NotFound())(F)

    def routes: HttpRoutes[F] =
      HttpRoutes.of[F] {
        case request@GET -> Root / path if List(".html", ".css", ".js", ".ico").exists(path.endsWith) =>
          static(path, request)
        case request@GET -> Root => static("index.html", request)
      }
