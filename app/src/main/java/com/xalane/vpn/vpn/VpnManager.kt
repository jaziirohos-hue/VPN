package com.xalane.vpn.vpn

import android.content.Context
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import com.wireguard.config.Interface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress

sealed interface VpnState {
    data object Disconnected : VpnState
    data object Connecting : VpnState
    data class Connected(val serverId: String, val sinceMillis: Long) : VpnState
    data class Failed(val message: String) : VpnState
}

/** Runs the WireGuard tunnel through the userspace Go backend. */
class VpnManager(context: Context) {

    private val backend by lazy { GoBackend(context.applicationContext) }

    private val _state = MutableStateFlow<VpnState>(VpnState.Disconnected)
    val state: StateFlow<VpnState> = _state

    private val tunnel = object : Tunnel {
        override fun getName() = "xalane"
        override fun onStateChange(newState: Tunnel.State) {
            if (newState == Tunnel.State.DOWN && _state.value is VpnState.Connected) {
                _state.value = VpnState.Disconnected
            }
        }
    }

    suspend fun connect(serverId: String, config: Config, blockAds: Boolean) = withContext(Dispatchers.IO) {
        _state.value = VpnState.Connecting
        try {
            val finalConfig = if (blockAds) withDns(config, AD_BLOCK_DNS) else config
            backend.setState(tunnel, Tunnel.State.UP, finalConfig)
            _state.value = VpnState.Connected(serverId, System.currentTimeMillis())
        } catch (e: Exception) {
            runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
            _state.value = VpnState.Failed(friendlyError(e))
        }
    }

    suspend fun disconnect() = withContext(Dispatchers.IO) {
        runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
        _state.value = VpnState.Disconnected
    }

    fun clearFailure() {
        if (_state.value is VpnState.Failed) _state.value = VpnState.Disconnected
    }

    /** Total bytes received and sent by the tunnel, or null when it is down. */
    fun traffic(): Pair<Long, Long>? = runCatching {
        val stats = backend.getStatistics(tunnel)
        stats.totalRx() to stats.totalTx()
    }.getOrNull()

    private fun withDns(config: Config, dns: List<String>): Config {
        val i = config.`interface`
        val iface = Interface.Builder().apply {
            setKeyPair(i.keyPair)
            addAddresses(i.addresses)
            addDnsServers(dns.map { InetAddress.getByName(it) })
            i.listenPort.ifPresent { setListenPort(it) }
            i.mtu.ifPresent { setMtu(it) }
            includeApplications(i.includedApplications)
            excludeApplications(i.excludedApplications)
        }.build()
        return Config.Builder().setInterface(iface).addPeers(config.peers).build()
    }

    private fun friendlyError(e: Exception): String = when {
        e.message?.contains("permission", ignoreCase = true) == true ->
            "Android did not allow the VPN. Tap Connect and accept the system prompt."
        else -> "Could not connect: ${e.message ?: e.javaClass.simpleName}"
    }

    companion object {
        /** AdGuard DNS, which blocks ads and trackers. */
        val AD_BLOCK_DNS = listOf("94.140.14.14", "94.140.15.15")
    }
}
