package com.xalane.vpn.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.wireguard.config.Config
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import java.io.File
import java.net.InetAddress

class ServerRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val configDir = File(context.filesDir, "configs").apply { mkdirs() }

    private val _servers = MutableStateFlow<List<Server>>(emptyList())
    val servers: StateFlow<List<Server>> = _servers

    suspend fun load() = withContext(Dispatchers.IO) {
        val bundled = context.assets.open("servers.json").use { input ->
            json.decodeFromString<List<ServerDto>>(input.readBytes().decodeToString())
        }.map { dto ->
            val config = dto.wireguardConfig?.let { text ->
                runCatching { Config.parse(text.byteInputStream()) }.getOrNull()
            }
            Server(
                id = dto.id,
                country = dto.country,
                countryCode = dto.countryCode,
                city = dto.city,
                lat = dto.lat,
                lon = dto.lon,
                latencyMs = if (config == null) dto.demoLatencyMs else null,
                latencyIsDemo = config == null,
                streaming = dto.streaming,
                config = config,
            )
        }
        _servers.value = loadCustom() + bundled
        measureLatency()
    }

    private fun loadCustom(): List<Server> =
        configDir.listFiles { f -> f.extension == "conf" }.orEmpty().sortedBy { it.name }.mapNotNull { file ->
            runCatching {
                val config = file.inputStream().use { Config.parse(it) }
                val host = config.peers.firstOrNull()?.endpoint?.orElse(null)?.host
                Server(
                    id = CUSTOM_PREFIX + file.nameWithoutExtension,
                    country = file.nameWithoutExtension,
                    countryCode = "",
                    city = host ?: "WireGuard",
                    custom = true,
                    config = config,
                )
            }.getOrNull()
        }

    /** Measures latency to servers that have a real endpoint. Demo servers keep their sample value. */
    private suspend fun measureLatency() = coroutineScope {
        val measured = _servers.value.filter { it.canConnect }.map { server ->
            async(Dispatchers.IO) {
                val host = server.config?.peers?.firstOrNull()?.endpoint?.orElse(null)?.host
                server.id to host?.let { ping(it) }
            }
        }.awaitAll().toMap()
        _servers.update { list ->
            list.map { s -> if (s.id in measured) s.copy(latencyMs = measured[s.id], latencyIsDemo = false) else s }
        }
    }

    private suspend fun ping(host: String): Int? = withTimeoutOrNull(3000) {
        runCatching {
            val address = InetAddress.getByName(host)
            val start = System.nanoTime()
            if (address.isReachable(2000)) ((System.nanoTime() - start) / 1_000_000).toInt() else null
        }.getOrNull()
    }

    /** Imports a WireGuard .conf file chosen by the user. Returns the new server. */
    suspend fun import(uri: Uri): Result<Server> = withContext(Dispatchers.IO) {
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
                ?: error("Could not read the file.")
            val config = runCatching { Config.parse(text.byteInputStream()) }
                .getOrElse { throw IllegalArgumentException("This file is not a valid WireGuard config.") }
            if (config.peers.isEmpty()) throw IllegalArgumentException("The config has no [Peer] section.")
            val name = uniqueName(sanitize(displayName(uri) ?: "My server"))
            File(configDir, "$name.conf").writeText(text)
            load()
            _servers.value.first { it.id == CUSTOM_PREFIX + name }
        }
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        if (!id.startsWith(CUSTOM_PREFIX)) return@withContext
        File(configDir, id.removePrefix(CUSTOM_PREFIX) + ".conf").delete()
        _servers.update { list -> list.filterNot { it.id == id } }
    }

    private fun displayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }?.substringBeforeLast('.')

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9 _.-]"), "").trim().take(32).ifBlank { "My server" }

    private fun uniqueName(base: String): String {
        var name = base
        var i = 2
        while (File(configDir, "$name.conf").exists()) name = "$base ${i++}"
        return name
    }

    companion object {
        const val CUSTOM_PREFIX = "custom:"
    }
}
