package com.example.robotpatrolapp

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.robotpatrolapp.model.RobotPosition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString


class MappingActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var isReceiving = true
    private val jsonParser = Json { ignoreUnknownKeys = true }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapping)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the button to go back to previous activity
        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)

        // Set an onClickListener
        btnBackToMain.setOnClickListener {
            // Create an Intent to start SecondActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        mapView = findViewById(R.id.mapPlaceholder)
        mapView.setStartingPosition(500f, 500f, 1.5708f)

        //Establishes the connection and runs the mapping
        CoroutineScope(Dispatchers.IO).launch {
            receiveData()
        }
    }

    private fun run(distTraveledCm: Float, angle: Float, obsDistance: Float, isNotSafe: Boolean) {
        // Passing new position data
        var newTheta = mapView.getPositionTheta() + angle * 0.1f
        newTheta %= 6.283185f
        val newX = mapView.getPositionX() + (distTraveledCm * cos(newTheta))
        val newY = mapView.getPositionY() + (distTraveledCm * sin(newTheta))

        // Update robot position with new values
        val newPosition = RobotPosition(newX, newY, newTheta)

        if(isNotSafe)
            mapView.pathPaint.color = Color.parseColor("#FF6666")
        else
            mapView.pathPaint.color = Color.parseColor("#66FF66")

        if(obsDistance > 50)
            mapView.addObstacle(newX + obsDistance * cos(newTheta), newY + obsDistance * sin(newTheta))
        mapView.updatePosition(newPosition)
    }

    private suspend fun receiveData() {
        val socket = SocketManager.socket
        if (socket == null) {
            withContext(Dispatchers.Main) {
                resetMapping()
            }
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                //val inputStream: InputStream = socket.getInputStream()
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                while (isReceiving) {
                    //val buffer = ByteArray(1024)
                    //val bytesRead = inputStream.read(buffer)
                    val jsonString = reader.readLine() ?: break
                    val robotData = jsonParser.decodeFromString<RobotData>(jsonString)

                    withContext(Dispatchers.Main) {
                        val distTraveledCm = (robotData.traveled.rear_left + robotData.traveled.rear_right + robotData.traveled.front_left + robotData.traveled.front_right) / 4
                        val angle = robotData.gyroscope.z
                        val obsDistance = robotData.distance.front
                        val isNotSafe = robotData.co2 > 1000  || robotData.flame || robotData.nh3 > 80
                        run(distTraveledCm.toFloat(), angle.toFloat(), obsDistance.toFloat(), isNotSafe)
                    }
                    delay(1000)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    resetMapping()
                }
            }
        }
    }

//    private suspend fun receiveData() {
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                //val inputStream: InputStream = socket.getInputStream()
//                var reader = """{"distance": {"front": -1, "right": -1, "left": -1, "rear": -1}, "bus_voltage": 0, "temperature": 29.5135, "timestamp": "2025-02-12T12:06:31", "gyroscope": {"y": -0.00290161, "x": 0.003512475, "z": 0.01893682}, "speed": {"front_left": 0.0, "rear_left": 0.0, "rear_right": 0.0, "front_right": 48.47324}, "humidity": 52.64163, "magnetometer": {"y": 27.63636, "x": -3.0, "z": 4.591837}, "power": 0.0, "co2_alarm": false, "pressure": 913.1486, "co2": 398, "nh3": 78.78351, "battery_percentage": 0, "raw_nh3": 212, "flame": true, "nh3_alarm": false, "traveled": {"front_left": 0.0, "rear_left": 0.0, "rear_right": 0.0, "front_right": 13.98408}, "current": 0, "accelerometer": {"y": -1.627904, "x": -0.392266, "z": 8.355266}}"""
//                var i = 1
//                while (i < 50) {
//                    i += 1
//                    //val buffer = ByteArray(1024)
//                    //val bytesRead = inputStream.read(buffer)
//                    val jsonString = reader
//                    val robotData = jsonParser.decodeFromString<RobotData>(jsonString)
//
//                    withContext(Dispatchers.Main) {
//                        val distTraveledCm = (robotData.traveled.rear_left + robotData.traveled.rear_right + robotData.traveled.front_left + robotData.traveled.front_right) / 4
//                        val angle = robotData.gyroscope.z
//                        val obsDistance = robotData.distance.front
//                        val isNotSafe = robotData.co2 > 1000  || robotData.flame || robotData.nh3 > 80
//                        run(distTraveledCm.toFloat(), angle.toFloat(), obsDistance.toFloat(), isNotSafe)
//                    }
//                    delay(100)
//                }
//                //val inputStream: InputStream = socket.getInputStream()
//                reader = """{"distance": {"front": -1, "right": -1, "left": -1, "rear": -1}, "bus_voltage": 0, "temperature": 29.5135, "timestamp": "2025-02-12T12:06:31", "gyroscope": {"y": -0.00290161, "x": 0.003512475, "z": 0.01893682}, "speed": {"front_left": 0.0, "rear_left": 0.0, "rear_right": 0.0, "front_right": 48.47324}, "humidity": 52.64163, "magnetometer": {"y": 27.63636, "x": -3.0, "z": 4.591837}, "power": 0.0, "co2_alarm": false, "pressure": 913.1486, "co2": 398, "nh3": 78.78351, "battery_percentage": 0, "raw_nh3": 212, "flame": false, "nh3_alarm": false, "traveled": {"front_left": 0.0, "rear_left": 0.0, "rear_right": 0.0, "front_right": 13.98408}, "current": 0, "accelerometer": {"y": -1.627904, "x": -0.392266, "z": 8.355266}}"""
//
//                while (i < 100){
//                    i += 1
//                    //val buffer = ByteArray(1024)
//                    //val bytesRead = inputStream.read(buffer)
//                    val jsonString = reader
//                    val robotData = jsonParser.decodeFromString<RobotData>(jsonString)
//
//                    withContext(Dispatchers.Main) {
//                        val distTraveledCm = (robotData.traveled.rear_left + robotData.traveled.rear_right + robotData.traveled.front_left + robotData.traveled.front_right) / 4
//                        val angle = robotData.gyroscope.z
//                        val obsDistance = robotData.distance.front
//                        val isNotSafe = robotData.co2 > 1000  || robotData.flame || robotData.nh3 > 80
//                        run(distTraveledCm.toFloat(), angle.toFloat(), obsDistance.toFloat(), isNotSafe)
//                    }
//                    delay(1000)
//                }
//            } catch (e: Exception) {
//                e.printStackTrace()
//                withContext(Dispatchers.Main) {
//                    resetMapping()
//                }
//            }
//        }
//    }



    private fun resetMapping() {
        mapView.resetCanvas()
    }
}

