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
import java.io.InputStream
import kotlin.math.cos
import kotlin.math.sin


class MappingActivity : AppCompatActivity() {

    private lateinit var mapView: MapView
    private var isReceiving = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_mapping)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the button "Voltar"
        val btnBackToMain = findViewById<Button>(R.id.btnBackToMain)

        // Set an onClickListener
        btnBackToMain.setOnClickListener {
            // Create an Intent to start SecondActivity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        CoroutineScope(Dispatchers.IO).launch {
            receiveData()
        }

        mapView = findViewById(R.id.mapPlaceholder)
        mapView.setStartingPosition(500f, 500f, 1.5708f)
    }

    // DELETE
        //val handler = Handler(Looper.getMainLooper())

//        val dist = 10f
//        var theta = 0f
//        val thetaValues = arrayOf(0f, 0f, 10f, 20f, 0f, 0f, 80f)
//        var i = 0

//        val updateRunnable = object : Runnable {
//            override fun run(distTra) {
//                // Simulating new robot position data
//                val newX = mapView.getPositionX() + (dist * cos(mapView.getPositionTheta()*theta/2))
//                val newY = mapView.getPositionY() + (dist * sin(mapView.getPositionTheta()*theta/2))
//                val newTheta = mapView.getPositionTheta() + theta
//
//                // Update robot position with new simulated values
//                val newPosition = RobotPosition(newX, newY, newTheta)
//                mapView.updatePosition(newPosition)
//
//                // Increment the angle and loop
//                if(i>thetaValues.size-1)
//                    i = 0
//                theta = thetaValues[i]
//                i += 1
//
//                handler.postDelayed(this, 100)  // Update every 100ms
//            }
//        }
//        handler.post(updateRunnable)


    private fun run(distTraveledCm: Float, angle: Float) {
        // Passing new position data
        val newX = mapView.getPositionX() + (distTraveledCm * cos(mapView.getPositionTheta()*angle/2))
        val newY = mapView.getPositionY() + (distTraveledCm * sin(mapView.getPositionTheta()*angle/2))
        var newTheta = mapView.getPositionTheta() + angle*1000
        newTheta %= 360

        // Update robot position with new values
        val newPosition = RobotPosition(newX, newY, newTheta)
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
                val inputStream: InputStream = socket.getInputStream()

                while (isReceiving) {
                    val buffer = ByteArray(1024)
                    val bytesRead = inputStream.read(buffer)

                    if (bytesRead > 0) {
                        val receivedMessage = String(buffer, 0, bytesRead).trim()
                        val parts = receivedMessage.split(",")

                        if (parts.size < 7) {
                            continue // Ignore incomplete messages
                        }

                        withContext(Dispatchers.Main) {
                            val distTraveledCm = parts.getOrNull(5)?.toFloatOrNull() ?: 0f
                            val angle = parts.getOrNull(6)?.toFloatOrNull() ?: 0f
                            run(distTraveledCm, angle)
                        }
                    }
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

