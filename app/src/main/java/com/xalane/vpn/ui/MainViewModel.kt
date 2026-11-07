package com.xalane.vpn.ui

import android.app.Application
import android.net.Uri
import android.net.VpnService
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.xalane.vpn.XalaneApplication
import com.xalane.vpn.data.AUTO_SERVER_ID
import com.xalane.vpn.data.AppSettings
import com.xalane.vpn.data.Server
import com.xalane.vpn.data.ThemeMode
import com.xalane.vpn.vpn.PublicIp
import com.xalane.vpn.vpn.VpnState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class UiState(
    val servers: List<Server> = emptyList(),
    val settings: AppSettings = AppSettings(),
    val vpn: VpnState = VpnState.Disconnected,
    val publicIp: String? = null,
    val ipLoading: Boolean = true,
    val downBytesPerSec: Long = 0,
    val upBytesPerSec: Long = 0,
) {
    val isAuto: Boolean get() = settings.selectedServerId == AUTO_SERVER_ID || servers.none { it.id == settings.selectedServerId }
    val selected: Server? get() = servers.firstOrNull { it.id == settings.selectedServerId }

    /** Fastest server that can connect, or the fastest Demo location when nothing is live yet. */
    val fastest: Server?
        get() = servers.filter { it.canConnect }.minByOrNull { it.latencyMs ?: Int.MAX_VALUE }
            ?: servers.minByOrNull { it.latencyMs ?: Int.MAX_VALUE }

    /** The server the next connection will use. */
    val target: Server? get() = if (isAuto) fastest else selected

    /** The server the tunnel is using right now, if connected. */
    val connectedServer: Server? get() = (vpn as? VpnState.Connected)?.let { c -> servers.firstOrNull { it.id == c.serverId } }
}

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val container = app as XalaneApplication
    private val serverRepo = container.servers
    private val settingsRepo = container.settings
    private val vpn = container.vpn

    private val ip = MutableStateFlow<Pair<String?, Boolean>>(null to true)
    private val speed = MutableStateFlow(0L to 0L)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<UiState> = combine(serverRepo.servers, settingsRepo.settings, vpn.state, ip, speed) { servers, settings, vpnState, ipPair, sp ->
        UiState(servers, settings, vpnState, ipPair.first, ipPair.second, sp.first, sp.second)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, UiState())

    init {
        viewModelScope.launch {
            serverRepo.load()
            val settings = settingsRepo.settings.first()
            if (settings.autoConnect && VpnService.prepare(getApplication()) == null && vpn.state.value == VpnState.Disconnected) {
                delay(300)
                connect()
            }
        }
        viewModelScope.launch {
            vpn.state.map { it::class }.distinctUntilChanged().collect {
                when (val s = vpn.state.value) {
                    is VpnState.Connected, VpnState.Disconnected -> refreshIp()
                    is VpnState.Failed -> {
                        _messages.emit(s.message)
                        vpn.clearFailure()
                    }
                    else -> Unit
                }
            }
        }
        viewModelScope.launch {
            var last: Pair<Long, Long>? = null
            while (true) {
                delay(1000)
                val now = if (vpn.state.value is VpnState.Connected) vpn.traffic() else null
                speed.value = if (now != null && last != null) {
                    (now.first - last.first).coerceAtLeast(0) to (now.second - last.second).coerceAtLeast(0)
                } else {
                    0L to 0L
                }
                last = now
            }
        }
    }

    private fun refreshIp() {
        viewModelScope.launch {
            ip.value = ip.value.first to true
            delay(800)
            ip.value = PublicIp.fetch() to false
        }
    }

    /**
     * Checks that the chosen server can connect. Shows a message and returns false when it can't,
     * so the screen only asks for VPN permission when a connection is possible.
     */
    fun canConnect(): Boolean {
        val target = state.value.target
        val problem = when {
            target == null -> "No servers yet. Import a WireGuard config to connect."
            !target.canConnect -> "${target.country} is a Demo location and isn't live yet. Import your own WireGuard config in Settings to connect today."
            else -> null
        }
        if (problem != null) _messages.tryEmit(problem)
        return problem == null
    }

    fun connect() {
        val s = state.value
        val target = s.target ?: return
        val config = target.config ?: return
        viewModelScope.launch { vpn.connect(target.id, config, s.settings.blockAds) }
    }

    fun disconnect() {
        viewModelScope.launch { vpn.disconnect() }
    }

    fun select(id: String) {
        viewModelScope.launch {
            settingsRepo.select(id)
            if (vpn.state.value is VpnState.Connected) {
                val server = if (id == AUTO_SERVER_ID) state.value.copy(settings = state.value.settings.copy(selectedServerId = id)).fastest
                else serverRepo.servers.value.firstOrNull { it.id == id }
                val config = server?.config
                if (server == null || config == null) {
                    _messages.emit("Still connected to the previous server. ${server?.country ?: "That location"} isn't live yet.")
                } else {
                    vpn.disconnect()
                    vpn.connect(server.id, config, state.value.settings.blockAds)
                }
            }
        }
    }

    fun toggleFavorite(id: String) = viewModelScope.launch { settingsRepo.toggleFavorite(id) }
    fun setAutoConnect(on: Boolean) = viewModelScope.launch { settingsRepo.setAutoConnect(on) }
    fun setTheme(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setTheme(mode) }

    fun setBlockAds(on: Boolean) = viewModelScope.launch {
        settingsRepo.setBlockAds(on)
        if (vpn.state.value is VpnState.Connected) _messages.emit("Reconnect to apply the new DNS setting.")
    }

    fun import(uri: Uri) = viewModelScope.launch {
        serverRepo.import(uri)
            .onSuccess { server ->
                settingsRepo.select(server.id)
                _messages.emit("Added ${server.country}. It's now your selected server.")
            }
            .onFailure { _messages.emit(it.message ?: "Could not import that file.") }
    }

    fun delete(id: String) = viewModelScope.launch {
        if (state.value.connectedServer?.id == id) vpn.disconnect()
        serverRepo.delete(id)
        if (state.value.settings.selectedServerId == id) settingsRepo.select(AUTO_SERVER_ID)
    }

    fun message(text: String) {
        _messages.tryEmit(text)
    }
}
