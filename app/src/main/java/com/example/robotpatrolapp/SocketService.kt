import com.example.robotpatrolapp.RobotData
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket
import java.util.concurrent.Executors

class SocketClient(private val ipAddress: String, private val port: Int) {
    private val jsonParser = Json { ignoreUnknownKeys = true }

    fun connect(onDataReceived: (RobotData) -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        executor.execute {
            try {
                val socket = Socket(ipAddress, port)
                val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
                val writer = PrintWriter(socket.getOutputStream(), true)

                println("Connected to Raspberry Pi at $ipAddress:$port")

                while (true) {
                    val jsonString = reader.readLine() ?: break
                    println("Received JSON: $jsonString")

                    // Parse JSON to RobotData object
                    val robotData = jsonParser.decodeFromString<RobotData>(jsonString)

                    // Pass parsed data to UI or another function
                    onDataReceived(robotData)
                }

                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}