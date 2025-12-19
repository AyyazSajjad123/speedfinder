package com.example.speedfinder.data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.InputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection

class SpeedTestEngine {

    // Cloudflare Speed Test (Global CDN - Bohot Fast aur Reliable hai)
    private val DOWNLOAD_URL = "https://speed.cloudflare.com/__down?bytes=20000000" // 20 MB File

    fun startDownloadTest(): Flow<Float> = flow {
        var connection: HttpsURLConnection? = null
        var inputStream: InputStream? = null

        try {
            Log.d("SpeedTest", "Connecting to Server...")
            val url = URL(DOWNLOAD_URL)
            connection = url.openConnection() as HttpsURLConnection

            // Connection settings
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.connect()

            Log.d("SpeedTest", "Connected! Status: ${connection.responseCode}")

            inputStream = connection.inputStream
            val buffer = ByteArray(1024 * 8) // 8KB ka chamach (Buffer)

            var bytesInLastSecond = 0L
            var startTime = System.currentTimeMillis()
            var lastUpdate = startTime

            // Download Shuru
            var bytesRead = inputStream.read(buffer)
            while (bytesRead != -1) {
                bytesInLastSecond += bytesRead

                val currentTime = System.currentTimeMillis()
                val timeDiff = currentTime - lastUpdate

                // Har 200ms (0.2 sec) baad meter ko update karo
                if (timeDiff >= 200) {
                    val speedBps = (bytesInLastSecond / (timeDiff / 1000.0)).toFloat()
                    val speedMbps = (speedBps * 8) / 1_000_000 // Convert to Mbps

                    Log.d("SpeedTest", "Speed: $speedMbps Mbps")
                    emit(speedMbps) // UI ko speed bhejo

                    bytesInLastSecond = 0
                    lastUpdate = currentTime
                }

                // 15 Second baad test rok do
                if (currentTime - startTime > 15000) break

                bytesRead = inputStream.read(buffer)
            }

        } catch (e: Exception) {
            Log.e("SpeedTest", "Error: ${e.message}")
            e.printStackTrace()
            emit(0f) // Error aaya to 0 show karo
        } finally {
            inputStream?.close()
            connection?.disconnect()
        }
    }.flowOn(Dispatchers.IO)
}