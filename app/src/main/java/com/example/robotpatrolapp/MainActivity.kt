package com.example.robotpatrolapp

import android.util.Log
import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils.substring
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
import com.google.gson.Gson
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var tvCarbonDioxide: TextView
    private lateinit var tvTemperature: TextView
    private lateinit var tvHumidity: TextView
    private lateinit var tvAmmonia: TextView
    private lateinit var tvFlame: TextView
    private lateinit var tvCarbonDioxideLabel: TextView
    private lateinit var tvAmmoniaLabel: TextView
    private lateinit var tvFlameLabel: TextView
    private var isReceiving = true // Control flag to stop receiving when needed
    private val channelId = "sensor_alert_channel" // Notification channel
    private val jsonParser = Json { ignoreUnknownKeys = true }
    private val gson = Gson()

    private val MAINTAG = "Main"
    private val VALUESTAG = "Values"

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(MAINTAG, "Creating" )
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
        tvCarbonDioxideLabel = findViewById(R.id.tvCarbonDioxideLabel)
        tvAmmoniaLabel = findViewById(R.id.tvAmmoniaLabel)
        tvFlameLabel = findViewById(R.id.tvFlameLabel)


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
    
    private var lastFlameState: Boolean = false // Armazena o último estado de chama

    private suspend fun receiveData() {
        val socket = SocketManager.socket
        if (socket == null) {
            withContext(Dispatchers.Main) {
                resetSensorValues()
            }
            return
        }
        Log.d(MAINTAG, "Receiving data" )

        withContext(Dispatchers.IO) {
            try {
                var reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                //val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
//                Log.d(MAINTAG, "Receiving and reading JSON: $reader" )

                // Debug: Check if data is available
//                if (socket.getInputStream().available() == 0) {
//                    Log.e(MAINTAG, "No data available yet")
//                } else {
//                    Log.d(MAINTAG, "Data detected!")
//                }
                //val jsonString = StringBuilder()

                while (isReceiving) {
                    Log.d(MAINTAG, "Started Loop")
                    reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    Log.d(MAINTAG, "Receiving and reading JSON: $reader" )

                    if (socket.getInputStream().available() == 0) {
                        Log.e(MAINTAG, "No data available yet")
                    } else {
                        Log.d(MAINTAG, "Data detected!")
                    }

                    val jsonString = StringBuilder()

                    //val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                    if(socket.isClosed || !socket.isConnected) Log.e(MAINTAG, "Socket error")

                    var charPrevious = 'a'
                    while (true) {
                        val char = reader.read().toChar()
                        if(charPrevious == '}'){
                            if(char == '}'){
                                jsonString.append(char)
                                break
                            }
                        }
                        charPrevious = char
                        Log.e(VALUESTAG, "$char")
                        //if (char == '$') break // End of stream
                        jsonString.append(char)
                        //if (char == '}') break // Assuming JSON ends with `}`
                    }
                    Log.d(MAINTAG, "Saiu: ")
                    val json = jsonString.toString()
                    Log.d(MAINTAG, "Received data: $json")


                    val robotData = RobotInfo()

                    withContext(Dispatchers.Main) {
//                        val co2 = extractJsonValue(json, "co2")
//                        val temp = extractJsonValue(json, "temperature")
//                        val humidity = extractJsonValue(json, "humidity")
//                        val nh3 = extractJsonValue(json, "nh3")
//                        val flame = extractJsonValue(json, "flame")

                        robotData.co2 = extractJsonValue(json, "co2")?.toDouble()!!
                        robotData.nh3 = extractJsonValue(json, "nh3")?.toDouble()!!
                        robotData.flame = extractJsonValue(json, "flame") == "true"
                        robotData.temperature = extractJsonValue(json, "temperature")?.toDouble()!!
                        robotData.humidity = extractJsonValue(json, "humidity")?.toDouble()!!

                        Log.d(MAINTAG, "Got value, nh3: ${robotData.nh3}")
                        Log.d(MAINTAG, "Got value, co2: ${robotData.co2}")
                        Log.d(MAINTAG, "Got value, temp: ${robotData.temperature}")
                        Log.d(MAINTAG, "Got value, humidity: ${robotData.humidity}")

                        // Atualizar valores de sensores
                        tvCarbonDioxide.text = String.format(locale = Locale.getDefault() ,"%.2f", robotData.co2)
                        tvTemperature.text = String.format(locale = Locale.getDefault(),"%.2f", robotData.temperature)
                        tvHumidity.text = String.format(locale = Locale.getDefault(), "%.2f", robotData.humidity)
                        tvAmmonia.text = String.format(locale = Locale.getDefault(), "%.2f", robotData.nh3)
                        tvFlame.text = if(robotData.flame) "Detectada" else "Não detectada"

//                        tvCarbonDioxide.text =  co2
//                        tvTemperature.text = temp
//                        tvHumidity.text = humidity
//                        tvAmmonia.text = nh3

                        delay(500)

                        //Verificar níveis e disparar notificações
                        launch {
                            if (robotData.co2 > 1000) {
                                tvCarbonDioxideLabel.setBackgroundColor(Color.RED)
                                showNotification(1, "Alerta de CO2!", "Nível de CO2 muito alto: ${robotData.co2} ppm.")
                                delay(3000)
                                tvCarbonDioxideLabel.setBackgroundColor(Color.GREEN)
                            }
                            if (robotData.nh3 > 80) {
                                tvAmmoniaLabel.setBackgroundColor(Color.RED)
                                showNotification(2, "Alerta de Amônia!", "Nível de NH3 muito alto: ${robotData.nh3} ppb.")
                                delay(3000)
                                tvAmmoniaLabel.setBackgroundColor(Color.GREEN)
                            }
                            if (robotData.flame) {
                                tvFlameLabel.setBackgroundColor(Color.RED)
                                showNotification(3, "Alerta de Chama!", "Presença de chama detectada!")
                                delay(3000)
                                tvFlameLabel.setBackgroundColor(Color.GREEN)
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
        lastFlameState = false
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

    private fun extractJsonValue(jsonString: String, key: String): String? {
        val regex = Regex(""""$key"\s*:\s*("(.*?)"|[\d.+-]+|true|false|null)""")
        val match = regex.find(jsonString)

        return match?.groups?.get(2)?.value ?: match?.groups?.get(1)?.value
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