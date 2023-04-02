package models
import io.circe.Codec

case class Orb(color: String, locX: Int, locY: Int, radius: Int)
    derives Codec.AsObject

object Orb {
  def apply(worldWidth: Int, worldHeight: Int): Orb = {
    val color = getRandomColor()
    val locX = scala.util.Random.nextInt(worldWidth)
    val locY = scala.util.Random.nextInt(worldHeight)
    val radius = 5
    Orb(color, locX, locY, radius)
  }

  private def getRandomColor(): String = {
    val r = scala.util.Random.nextInt(151) + 50
    val g = scala.util.Random.nextInt(151) + 50
    val b = scala.util.Random.nextInt(151) + 50
    s"rgb($r, $g, $b)"
  }

  def generateOrbs(settings: Settings): List[Orb] = {
    List.fill(settings.defaultOrbs)(
      Orb(settings.worldWidth, settings.worldHeight)
    )
  }
}
