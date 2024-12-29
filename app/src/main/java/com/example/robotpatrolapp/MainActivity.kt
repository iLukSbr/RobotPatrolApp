package com.example.robotpatrolapp

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import java.io.InputStream
import androidx.core.app.ActivityCompat


class MainActivity : AppCompatActivity() {

    private lateinit var tvCarbonDioxide: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvAmmonia: TextView
    private lateinit var tvFlame: TextView
    private var isReceiving = true // Control flag to stop receiving when needed
    private val channelId = "sensor_alert_channel" // Notification channel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1001 // Código de requisição (pode ser qualquer número)
                )
            }
        }

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

        createNotificationChannel()

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
    
    private var lastFlameState: Int? = null // Armazena o último estado de chama

    private suspend fun receiveData() {
        val socket = SocketManager.socket
        if (socket == null) {
            withContext(Dispatchers.Main) {
                resetSensorValues()
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

                        if (parts.size < 5) {
                            continue // Ignorar mensagens incompletas
                        }

                        withContext(Dispatchers.Main) {
                            val co2 = parts.getOrNull(0)?.toFloatOrNull() ?: 0f
                            val temp = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
                            val humidity = parts.getOrNull(2)?.toFloatOrNull() ?: 0f
                            val nh3 = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
                            val flame = parts.getOrNull(4)?.toIntOrNull() ?: -1

                            // Atualizar valores de sensores
                            tvCarbonDioxide.text = String.format("%.2f ppm", co2)
                            tvTemperature.text = String.format("%.2f °C", temp)
                            tvHumidity.text = String.format("%.2f %%", humidity)
                            tvAmmonia.text = String.format("%.2f ppm", nh3)

                            // Validar e atualizar estado da chama
                            if (flame in 0..1) {
                                if (lastFlameState != flame) {
                                    lastFlameState = flame
                                    tvFlame.text = if (flame == 1) "Detectada" else "Não detectada"
                                }
                            }

                            // Verificar níveis e disparar notificações
                            launch {
                                if (co2 > 1000) {
                                    showNotification(1, "Alerta de CO2!", "Nível de CO2 muito alto: $co2 ppm.")
                                    delay(3000)
                                }
                                if (nh3 > 50) {
                                    showNotification(2, "Alerta de Amônia!", "Nível de NH3 muito alto: $nh3 ppm.")
                                    delay(3000)
                                }
                                if (flame == 1) {
                                    showNotification(3, "Alerta de Chama!", "Presença de chama detectada!")
                                    delay(3000)
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    resetSensorValues()
                }
            }
        }
    }

    private fun resetSensorValues() {
        tvCarbonDioxide.text = "NaN"
        tvTemperature.text = "NaN"
        tvHumidity.text = "NaN"
        tvAmmonia.text = "NaN"
        tvFlame.text = "NaN"
        lastFlameState = null
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Alertas de Sensores",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun showNotification(notificationId: Int, title: String, message: String) {
        // Solicitar permissão (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return // Não exibir notificações se a permissão não foi concedida
            }
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }
}