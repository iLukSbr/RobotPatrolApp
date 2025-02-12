package com.example.robotpatrolapp.model

data class RobotPosition(
    var x: Float,  // X coordinate
    var y: Float,  // Y coordinate
    var theta: Float  // Orientation in degrees (0° = facing right, 90° = facing up)
)