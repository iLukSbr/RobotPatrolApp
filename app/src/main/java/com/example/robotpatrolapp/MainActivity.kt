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

    private lateinit var tvAmmonia: TextView
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

        tvAmmonia = findViewById(R.id.tvAmmoniaValue)

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
                tvAmmonia.text = "0"
            }
            return
        }

        try {
            val inputStream: InputStream = socket.getInputStream()

            while (isReceiving) {
                val buffer = ByteArray(1024)
                val bytesRead = inputStream.read(buffer)

                if (bytesRead > 0) {
                    val receivedMessage = String(buffer, 0, bytesRead)

                    withContext(Dispatchers.Main) {
                        tvAmmonia.text = receivedMessage
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                tvAmmonia.text = "NaN"
            }
        }
    }
    
}