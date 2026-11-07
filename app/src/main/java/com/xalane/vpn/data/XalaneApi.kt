package com.xalane.vpn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class ApiServer(
    val id: String,
    val country: String,
    @SerialName("country_code") val countryCode: String,
    val city: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val host: String,
    val online: Boolean,
    val load: Int = 0,
    val streaming: Boolean = false,
)

@Serializable
data class RegisterRequest(@SerialName("public_key") val publicKey: String, val platform: String = "android")

@Serializable
data class RegisterResponse(@SerialName("device_id") val deviceId: String, val token: String)

@Serializable
data class ConnectRequest(@SerialName("server_id") val serverId: String)

@Serializable
data class ConnectResponse(
    @SerialName("server_id") val serverId: String,
    @SerialName("interface") val iface: ApiInterface,
    val peer: ApiPeer,
)

@Serializable
data class ApiInterface(val address: String, val dns: List<String>)

@Serializable
data class ApiPeer(
    @SerialName("public_key") val publicKey: String,
    val endpoint: String,
    @SerialName("allowed_ips") val allowedIps: List<String>,
    @SerialName("persistent_keepalive") val persistentKeepalive: Int = 25,
)

/** An error the API answered with. [message] is the API's own explanation. */
class ApiException(val code: Int, message: String) : IOException(message)

/** Client for the Xalane control API (see backend/). */
class XalaneApi(baseUrl: String) {
    private val base = baseUrl.trimEnd('/')
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    suspend fun servers(): List<ApiServer> = json.decodeFromString(call("GET", "/v1/servers"))

    suspend fun register(publicKey: String): RegisterResponse =
        json.decodeFromString(call("POST", "/v1/devices", json.encodeToString(RegisterRequest.serializer(), RegisterRequest(publicKey))))

    suspend fun connect(token: String, serverId: String): ConnectResponse =
        json.decodeFromString(call("POST", "/v1/connect", json.encodeToString(ConnectRequest.serializer(), ConnectRequest(serverId)), token))

    suspend fun deleteDevice(token: String) {
        call("DELETE", "/v1/devices/me", token = token)
    }

    private suspend fun call(method: String, path: String, body: String? = null, token: String? = null): String =
        withContext(Dispatchers.IO) {
            val conn = URL(base + path).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method
                conn.connectTimeout = 10_000
                conn.readTimeout = 15_000
                conn.setRequestProperty("Accept", "application/json")
                token?.let { conn.setRequestProperty("Authorization", "Bearer $it") }
                if (body != null) {
                    conn.doOutput = true
                    conn.setRequestProperty("Content-Type", "application/json")
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }
                val code = conn.responseCode
                val text = (if (code < 400) conn.inputStream else conn.errorStream)?.use { it.readBytes().decodeToString() }.orEmpty()
                if (code >= 400) throw ApiException(code, detail(text) ?: "Server error ($code)")
                text
            } finally {
                conn.disconnect()
            }
        }

    private fun detail(body: String): String? = runCatching {
        json.parseToJsonElement(body).jsonObject["detail"]?.jsonPrimitive?.content
    }.getOrNull()
}

/** Builds a WireGuard config file from the API's answer. The private key never leaves the phone. */
fun wireGuardConfigText(privateKey: String, r: ConnectResponse): String = buildString {
    appendLine("[Interface]")
    appendLine("PrivateKey = $privateKey")
    appendLine("Address = ${r.iface.address}")
    if (r.iface.dns.isNotEmpty()) appendLine("DNS = ${r.iface.dns.joinToString(", ")}")
    appendLine()
    appendLine("[Peer]")
    appendLine("PublicKey = ${r.peer.publicKey}")
    appendLine("Endpoint = ${r.peer.endpoint}")
    appendLine("AllowedIPs = ${r.peer.allowedIps.joinToString(", ")}")
    appendLine("PersistentKeepalive = ${r.peer.persistentKeepalive}")
}
