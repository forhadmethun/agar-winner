package models

case class PlayerConfig(defaultSpeed: Int, defaultZoom: Double)

case class PlayerData(playerName: String, settings: Settings) {
  val uid: String = java.util.UUID.randomUUID().toString
  val locX: Int = scala.util.Random.nextInt(settings.worldWidth) + 100
  val locY: Int = scala.util.Random.nextInt(settings.worldHeight) + 100
  val radius: Int = settings.defaultSize
  val color: String = getRandomColor()
  var score: Int = 0
  var orbsAbsorbed: Int = 0

  private def getRandomColor(): String = {
    val r = scala.util.Random.nextInt(150) + 50
    val g = scala.util.Random.nextInt(150) + 50
    val b = scala.util.Random.nextInt(150) + 50
    s"rgb($r,$g,$b)"
  }
}

case class Player(playerConfig: PlayerConfig, playerData: PlayerData)
