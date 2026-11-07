package com.xalane.vpn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import com.xalane.vpn.ui.components.PillNavBar
import com.xalane.vpn.ui.components.Tab
import com.xalane.vpn.ui.screens.HomeScreen
import com.xalane.vpn.ui.screens.PremiumScreen
import com.xalane.vpn.ui.screens.ServersScreen
import com.xalane.vpn.ui.screens.SettingsScreen
import com.xalane.vpn.ui.screens.SplashScreen
import com.xalane.vpn.ui.theme.X
import com.xalane.vpn.ui.theme.XalaneTheme
import com.xalane.vpn.vpn.VpnState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Renders each screen to app/build/outputs/roborazzi so the UI can be reviewed without a device. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w380dp-h820dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private fun shot(name: String, dark: Boolean = true, tab: Tab? = Tab.HOME, content: @Composable () -> Unit) {
        compose.setContent {
            XalaneTheme(dark = dark) {
                Column(Modifier.fillMaxSize().background(X.colors.bg)) {
                    Column(Modifier.weight(1f)) { content() }
                    if (tab != null) PillNavBar(tab, {}, Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }

    @Test fun splash() = shot("1_splash", tab = null) { SplashScreen {} }

    @Test fun homeDisconnected() = shot("2_home_disconnected") {
        HomeScreen(SampleData.state(), {}, {}, {}, {})
    }

    @Test fun homeConnected() = shot("3_home_connected") {
        val s = SampleData.state().copy(
            vpn = VpnState.Connected("de-fra", System.currentTimeMillis() - 2_189_000),
            publicIp = "185.220.14.62",
            downBytesPerSec = 6_025_000,
            upBytesPerSec = 2_712_000,
        )
        HomeScreen(s, {}, {}, {}, {})
    }

    @Test fun homeConnectedLight() = shot("4_home_connected_light", dark = false) {
        val s = SampleData.state().copy(vpn = VpnState.Connected("de-fra", System.currentTimeMillis() - 754_000), publicIp = "185.220.14.62")
        HomeScreen(s, {}, {}, {}, {})
    }

    @Test fun servers() = shot("5_servers", tab = Tab.SERVERS) { ServersScreen(SampleData.state(), {}, {}, {}) }

    @Test fun settings() = shot("6_settings", tab = Tab.SETTINGS) { SettingsScreen(SampleData.state(), {}, {}, {}, {}, {}) }

    @Test fun premium() = shot("7_premium", tab = Tab.PREMIUM) { PremiumScreen({}, {}) }
}
