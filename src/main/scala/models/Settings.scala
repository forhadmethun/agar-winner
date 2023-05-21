package models

case object Settings {
  val defaultOrbs: Int = 150
  val defaultOrbRadius: Int = 5
  val defaultSpeed: Int = 6
  val defaultSize: Int = 6
  val defaultZoom: Double = 1.5
  val worldWidth: Int = 3840
  val worldHeight: Int = 2160
  val zoomThreshold: Double = 1
  val zoomValueChange: Double = 0.001
  val speedThreshold: Double = 0.005 
  val speedValueChange: Double = 0.005 
  val radiusValueChange: Double = 0.25
  val proximityThreshold: Double = 35
}
