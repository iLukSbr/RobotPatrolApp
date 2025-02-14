package com.example.robotpatrolapp

import android.content.Intent
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

    private fun run(distTraveledCm: Float, angle: Float, obsDistance: Float) {
        // Passing new position data
        val newX = mapView.getPositionX() + (distTraveledCm * cos(mapView.getPositionTheta()*angle/2))
        val newY = mapView.getPositionY() + (distTraveledCm * sin(mapView.getPositionTheta()*angle/2))
        var newTheta = mapView.getPositionTheta() + angle*1000
        newTheta %= 360

        // Update robot position with new values
        val newPosition = RobotPosition(newX, newY, newTheta)
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
                        val distTraveledCm = robotData.traveled.front_right
                        val angle = robotData.gyroscope.z
                        val obsDistance = robotData.distance.front
                        run(distTraveledCm.toFloat(), angle.toFloat(), obsDistance.toFloat())
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

    private fun resetMapping() {
        mapView.resetCanvas()
    }
}

