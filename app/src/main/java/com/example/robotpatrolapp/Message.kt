package com.example.robotpatrolapp
import org.json.JSONObject
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Serializable
data class RobotData(
    val distance: Distance,
    val bus_voltage: Double,
    val temperature: Double,
    val timestamp: String,
    val gyroscope: Vector3D,
    val speed: WheelSpeeds,
    val humidity: Double,
    val magnetometer: Vector3D,
    val power: Double,
    val co2_alarm: Boolean,
    val pressure: Double,
    val co2: Int,
    val nh3: Double,
    val battery_percentage: Double,
    val raw_nh3: Int,
    val flame: Boolean,
    val nh3_alarm: Boolean,
    val traveled: WheelSpeeds,
    val current: Double,
    val accelerometer: Vector3D
)

@Serializable
data class Distance(val front: Double, val right: Double, val left: Double, val rear: Double)

@Serializable
data class Vector3D(val x: Double, val y: Double, val z: Double)

@Serializable
data class WheelSpeeds(
    val front_left: Double,
    val rear_left: Double,
    val rear_right: Double,
    val front_right: Double
)

private val json = Json { ignoreUnknownKeys = true }

// Function to parse JSON
fun parseJson(jsonString: String): RobotData {
    return json.decodeFromString(jsonString)
}

/*
{"distance": {"front": -1, "right": -1, "left": -1, "rear": -1},
"bus_voltage": 0, "temperature": 29.5135,
"timestamp": "2025-02-12T12:06:31",
"gyroscope": {"y": -0.00290161, "x": 0.003512475, "z": 0.01893682},
"speed": {"front_left": 0.0, "rear_left": 0.0, "rear_right": 0.0, "front_right": 48.47324},
"humidity": 52.64163,
"magnetometer": {"y": 27.63636, "x": -3.0, "z": 4.591837},
"power": 0.0,
"co2_alarm": false,
"pressure": 913.1486,
"co2": 398,
"nh3": 78.78351,
"battery_percentage": 0,
"raw_nh3": 212,
"flame": true,
"nh3_alarm": false,
"traveled": {"front_left": 0.0, "rear_left": 0.0, "rear_right": 0.0, "front_right": 13.98408},
"current": 0,
"accelerometer": {"y": -1.627904, "x": -0.392266, "z": 8.355266}}
 */