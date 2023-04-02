package models

case class Settings(
    defaultOrbs: Int = 50,
    defaultSpeed: Int = 6,
    defaultSize: Int = 6,
    defaultZoom: Double = 1.5,
    worldWidth: Int = 1440,
    worldHeight: Int = 1440
)
