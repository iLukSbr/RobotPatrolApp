package com.example.robotpatrolapp

data class RobotInfo (
    var distanceTraveledRearLeft : Double = 0.0,
    var distanceTraveledRearRight : Double = 0.0,
    var distanceTraveledFrontLeft : Double = 0.0,
    var co2 : Double = 0.0,
    var nh3 : Double = 0.0,
    var flame : Boolean = false,
    var temperature : Double = 0.0,
    var humidity : Double = 0.0,
    var gyroscopeZ : Double = 0.0,
    var objectDist : Double = 0.0
)

