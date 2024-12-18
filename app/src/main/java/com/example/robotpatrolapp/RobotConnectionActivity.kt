package com.example.robotpatrolapp

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.*
import android.widget.Toast
import java.net.Socket

class RobotConnectionActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_robot_connection)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Find the button "Conectar"
        val btnToMain = findViewById<Button>(R.id.btnConnect)
        val etIp = findViewById<EditText>(R.id.etIpAddress)
        val etPort = findViewById<EditText>(R.id.etPortNumber)
        val tvStatus = findViewById<TextView>(R.id.tvStatusMessage)

        // Set an onClickListener
        btnToMain.setOnClickListener {
            val ip = etIp.text.toString()
            val port = etPort.text.toString().toInt()

            CoroutineScope(Dispatchers.IO).launch {
                val socket = SocketManager.connect(ip, port)
                if (socket != null) {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Conectado!"
                        tvStatus.setTextColor(Color.GREEN)

                        // Navigate to MainActivity
                        val intent = Intent(this@RobotConnectionActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        tvStatus.text = "Conexão falhou"
                        tvStatus.setTextColor(Color.RED)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        //SocketManager.disconnect()
    }
}

object SocketManager {
    var socket: Socket? = null
    var ip: String = ""
    var port: Int = 0

    fun connect(ip: String, port: Int): Socket? {
        return try {
            this.ip = ip
            this.port = port
            socket = Socket(ip, port)
            socket
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun disconnect() {
        try {
            socket?.close()
            socket = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
