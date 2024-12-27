package com.example.robotpatrolapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import java.io.InputStream

class MainActivity : AppCompatActivity() {

    private lateinit var tvCarbonDioxide: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvAmmonia: TextView
    private lateinit var tvFlame: TextView
    private var isReceiving = true // Control flag to stop receiving when needed

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvCarbonDioxide = findViewById(R.id.tvCarbonDioxideValue)
        tvTemperature = findViewById(R.id.tvTempValue)
        tvHumidity = findViewById(R.id.tvHumidityValue)
        tvAmmonia = findViewById(R.id.tvAmmoniaValue)
        tvFlame = findViewById(R.id.tvFlameValue)

        CoroutineScope(Dispatchers.IO).launch {
            receiveData()
        }

        // Find the button "Ver Mapeamento"
        val btnToMapping = findViewById<Button>(R.id.btMainToMap)

        // Set an onClickListener
        btnToMapping.setOnClickListener {
            // Create an Intent to start SecondActivity
            val intent = Intent(this, MappingActivity::class.java)
            startActivity(intent)
        }
    }

    private suspend fun receiveData() {
        val socket = SocketManager.socket
        if (socket == null) {
            withContext(Dispatchers.Main) {
                tvCarbonDioxide.text = "0"
                tvTemperature.text = "0"
                tvHumidity.text = "0"
                tvAmmonia.text = "0"
                tvFlame.text = "0"
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
                        val receivedMessage = String(buffer, 0, bytesRead)

                        // Separates message based on ','
                        val parts = receivedMessage.split(",")

                        withContext(Dispatchers.Main) {
                            tvCarbonDioxide.text = parts.getOrNull(0) ?: "NaN"
                            tvTemperature.text = parts.getOrNull(1) ?: "NaN"
                            tvHumidity.text = parts.getOrNull(2) ?: "NaN"
                            tvAmmonia.text = parts.getOrNull(3) ?: "NaN"
                            tvFlame.text = parts.getOrNull(4) ?: "NaN"
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    tvCarbonDioxide.text = "NaN"
                    tvTemperature.text = "NaN"
                    tvHumidity.text = "NaN"
                    tvAmmonia.text = "NaN"
                    tvFlame.text = "NaN"
                }
            }
        }
    }

}