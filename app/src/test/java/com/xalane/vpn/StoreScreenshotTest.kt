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
import com.xalane.vpn.data.AppSettings
import com.xalane.vpn.data.Server
import com.xalane.vpn.ui.UiState
import com.xalane.vpn.ui.components.PillNavBar
import com.xalane.vpn.ui.components.Tab
import com.xalane.vpn.ui.screens.HomeScreen
import com.xalane.vpn.ui.screens.ServersScreen
import com.xalane.vpn.ui.screens.SettingsScreen
import com.xalane.vpn.ui.theme.X
import com.xalane.vpn.ui.theme.XalaneTheme
import com.xalane.vpn.vpn.VpnState
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Screens as the release build shows them (no Demo content), used for the Play Store listing. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w380dp-h820dp-xxhdpi")
class StoreScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Before fun releaseMode() { AppFlags.demoContent = false }
    @After fun restore() { AppFlags.demoContent = BuildConfig.DEMO_CONTENT }

    private val servers = listOf(
        Server("custom:Home server", "Home server", "", "203.0.113.10", latencyMs = 18, custom = true),
        Server("custom:Office", "Office", "", "198.51.100.24", latencyMs = 34, custom = true),
        Server("custom:Travel", "Travel", "", "192.0.2.77", latencyMs = 61, custom = true),
    )
    private fun state() = UiState(
        servers = servers,
        settings = AppSettings(selectedServerId = "custom:Home server", autoConnect = true, blockAds = true),
        publicIp = "102.68.77.13",
        ipLoading = false,
    )

    private fun shot(name: String, tab: Tab, dark: Boolean = true, content: @Composable () -> Unit) {
        compose.setContent {
            XalaneTheme(dark = dark) {
                Column(Modifier.fillMaxSize().background(X.colors.bg)) {
                    Column(Modifier.weight(1f)) { content() }
                    PillNavBar(tab, {}, Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
                }
            }
        }
        compose.mainClock.advanceTimeBy(500)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/store/$name.png")
    }

    @Test fun connected() = shot("1_connected", Tab.HOME) {
        HomeScreen(
            state().copy(
                vpn = VpnState.Connected("custom:Home server", System.currentTimeMillis() - 2_189_000),
                publicIp = "203.0.113.10", downBytesPerSec = 6_025_000, upBytesPerSec = 2_712_000,
            ),
            {}, {}, {}, {},
        )
    }

    @Test fun disconnected() = shot("2_disconnected", Tab.HOME) { HomeScreen(state(), {}, {}, {}, {}) }

    @Test fun servers() = shot("3_servers", Tab.SERVERS) { ServersScreen(state(), {}, {}, {}) }

    @Test fun settings() = shot("4_settings", Tab.SETTINGS) { SettingsScreen(state(), {}, {}, {}, {}, {}) }

    @Test fun light() = shot("5_light", Tab.HOME, dark = false) {
        HomeScreen(state().copy(vpn = VpnState.Connected("custom:Home server", System.currentTimeMillis() - 754_000), publicIp = "203.0.113.10", downBytesPerSec = 3_100_000, upBytesPerSec = 900_000), {}, {}, {}, {})
    }

    @Test fun empty() = shot("6_empty", Tab.SERVERS) {
        ServersScreen(UiState(ipLoading = false), {}, {}, {})
    }
}
