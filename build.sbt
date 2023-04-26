val projectVersion = "0.1.0"
val scala3Version = "3.2.2"
val Http4sVersion = "0.23.13"
val CirceVersion = "0.14.4"
val log4CatsVersion = "2.5.0"
val ScribeVersion = "3.10.0"

lazy val root = (project in file("."))
  .settings(
    name := "agar-winner",
    version := projectVersion,
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "0.7.29" % Test,
      "org.http4s" %% "http4s-ember-server" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-core" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.typelevel" %% "log4cats-core" % log4CatsVersion,
      "com.outr" %% "scribe-slf4j" % ScribeVersion
    )
  )
