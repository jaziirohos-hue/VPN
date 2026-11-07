package com.xalane.vpn

import android.app.Application
import com.xalane.vpn.data.ServerRepository
import com.xalane.vpn.data.SettingsRepository
import com.xalane.vpn.vpn.VpnManager

class XalaneApplication : Application() {
    val servers by lazy { ServerRepository(this) }
    val settings by lazy { SettingsRepository(this) }
    val vpn by lazy { VpnManager(this) }
}
