package com.xalane.vpn.data

import com.wireguard.config.Config
import kotlinx.serialization.Serializable

/** One entry of assets/servers.json. These are Demo locations until the Xalane backend provides real configs. */
@Serializable
data class ServerDto(
    val id: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val demoLatencyMs: Int? = null,
    val streaming: Boolean = false,
    /** Optional full WireGuard config text. When present the server can connect. */
    val wireguardConfig: String? = null,
)

data class Server(
    val id: String,
    val country: String,
    val countryCode: String,
    val city: String,
    val lat: Double? = null,
    val lon: Double? = null,
    val latencyMs: Int? = null,
    /** True when the latency is sample data, not a measurement. */
    val latencyIsDemo: Boolean = false,
    val streaming: Boolean = false,
    /** True for configs the user imported. */
    val custom: Boolean = false,
    val config: Config? = null,
    /** True for servers listed by the Xalane API. They connect through the API, not a stored config. */
    val remote: Boolean = false,
    val online: Boolean = false,
    val host: String? = null,
    val load: Int? = null,
) {
    val canConnect: Boolean get() = config != null || (remote && online)
    val isDemo: Boolean get() = !custom && !remote && config == null
}

const val AUTO_SERVER_ID = "auto"
