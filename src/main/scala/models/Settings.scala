package models

case object Settings {
  val defaultOrbs: Int = 50
  val defaultOrbRadius: Int = 5
  val defaultSpeed: Int = 6
  val defaultSize: Int = 6
  val defaultZoom: Double = 1.5
  val worldWidth: Int = 1440
  val worldHeight: Int = 1440
  val zoomThreshold: Double = 1
  val zoomValueChange: Double = 0.001
  val speedThreshold: Double = 0.005 
  val speedValueChange: Double = 0.005 
  val radiusValueChange: Double = 0.75
}
