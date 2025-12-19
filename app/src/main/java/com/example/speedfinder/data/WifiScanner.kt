package com.example.speedfinder.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.InetAddress

class WifiScanner(private val context: Context) {

    // 1. Apna Local IP nikalna (e.g., 192.168.1.5)
    private fun getLocalIpAddress(): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return null
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork) ?: return null

            for (linkAddress in linkProperties.linkAddresses) {
                val address = linkAddress.address
                if (address is Inet4Address && !address.isLoopbackAddress) {
                    return address.hostAddress
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    // 2. Subnet nikalna (e.g., "192.168.1.")
    private fun getSubnetAddress(ip: String): String {
        return ip.substring(0, ip.lastIndexOf(".") + 1)
    }

    // 3. MAIN MAGIC: Parallel Scanning (Fast)
    suspend fun scanNetwork(): List<String> = withContext(Dispatchers.IO) {
        val myIp = getLocalIpAddress() ?: return@withContext emptyList()
        val subnet = getSubnetAddress(myIp)
        val foundDevices = mutableListOf<String>()

        // 1 se 254 tak saare IPs ki list banao
        val ipsToScan = (1..254).map { "$subnet$it" }

        // Sabko ek sath (Parallel) Ping karo
        val tasks = ipsToScan.map { ip ->
            async {
                if (isReachable(ip)) {
                    if (ip == myIp) "$ip (Me)" else ip
                } else {
                    null
                }
            }
        }

        // Result jama karo
        tasks.awaitAll().filterNotNull().toCollection(foundDevices)
        return@withContext foundDevices
    }

    // Ping Logic (Native Command use karna zyada reliable hai)
    private fun isReachable(ip: String): Boolean {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 -w 1 $ip")
            val returnVal = process.waitFor()
            returnVal == 0
        } catch (e: Exception) {
            false
        }
    }
}