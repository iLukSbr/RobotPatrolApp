package com.example.robotpatrolapp.model

data class RobotPosition(
    var x: Double,  // X coordinate
    var y: Double,  // Y coordinate
    var theta: Double  // Orientation in degrees (0° = facing right, 90° = facing up)
)