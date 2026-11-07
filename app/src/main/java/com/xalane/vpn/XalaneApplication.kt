package com.xalane.vpn

import android.app.Application
import com.xalane.vpn.data.DeviceIdentity
import com.xalane.vpn.data.ServerRepository
import com.xalane.vpn.data.XalaneApi
import com.xalane.vpn.data.SettingsRepository
import com.xalane.vpn.vpn.VpnManager

class XalaneApplication : Application() {
    val api by lazy { BuildConfig.API_BASE_URL.takeIf { it.isNotBlank() }?.let { XalaneApi(it) } }
    val servers by lazy { ServerRepository(this, api, DeviceIdentity(this)) }
    val settings by lazy { SettingsRepository(this) }
    val vpn by lazy { VpnManager(this) }
}
