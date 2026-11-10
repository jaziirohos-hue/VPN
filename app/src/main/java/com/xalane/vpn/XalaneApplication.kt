package com.xalane.vpn

import android.app.Application
import android.util.Log
import com.wireguard.android.backend.GoBackend
import com.xalane.vpn.data.DeviceIdentity
import com.xalane.vpn.data.ServerRepository
import com.xalane.vpn.data.SettingsRepository
import com.xalane.vpn.ui.UiState
import com.xalane.vpn.vpn.VpnManager
import com.xalane.vpn.vpn.VpnState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class XalaneApplication : Application() {
    val servers by lazy { ServerRepository(this, BuildConfig.API_BASE_URL, DeviceIdentity(this)) }
    val settings by lazy { SettingsRepository(this) }
    val vpn by lazy { VpnManager(this) }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Android's "Always-on VPN" (and "Block connections without VPN") starts our VPN service
        // without the app open. Connect to the user's chosen server so the phone isn't left offline.
        GoBackend.setAlwaysOnCallback { appScope.launch { connectForAlwaysOn() } }
    }

    private suspend fun connectForAlwaysOn() {
        if (vpn.state.value is VpnState.Connected || vpn.state.value is VpnState.Connecting) return
        val saved = settings.settings.first()
        if (servers.servers.value.isEmpty()) servers.setApiUrl(saved.apiUrl)
        val target = UiState(servers = servers.servers.value, settings = saved).target
        if (target == null || !target.canConnect) {
            Log.w("Xalane", "Always-on VPN started but no server is available to connect to")
            return
        }
        vpn.connect(target.id, saved.blockAds) { target.config ?: servers.remoteConfig(target) }
    }
}
