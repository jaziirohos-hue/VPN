package com.xalane.vpn.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

object PublicIp {
    /** Looks up the device's current public IP address. Returns null when offline. */
    suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        runCatching {
            val conn = URL("https://api.ipify.org").openConnection() as HttpURLConnection
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            try {
                conn.inputStream.use { it.readBytes().decodeToString().trim() }.takeIf { it.length in 3..45 }
            } finally {
                conn.disconnect()
            }
        }.getOrNull()
    }
}
