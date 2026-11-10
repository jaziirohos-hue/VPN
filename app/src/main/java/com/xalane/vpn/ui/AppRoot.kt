package com.xalane.vpn.ui

import android.app.Activity
import android.net.VpnService
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import com.xalane.vpn.ui.components.PillNavBar
import com.xalane.vpn.ui.components.Tab
import com.xalane.vpn.ui.screens.HomeScreen
import com.xalane.vpn.ui.screens.PremiumScreen
import com.xalane.vpn.ui.screens.ServersScreen
import com.xalane.vpn.ui.screens.SettingsScreen
import com.xalane.vpn.ui.screens.SplashScreen
import com.xalane.vpn.ui.theme.X
import com.xalane.vpn.vpn.VpnState

@Composable
fun AppRoot(vm: MainViewModel, state: UiState) {
    val context = LocalContext.current
    val c = X.colors
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.messages.collect { snackbar.showSnackbar(it) }
    }

    val vpnPermission = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) vm.connect()
        else vm.message("Xalane needs VPN permission to connect.")
    }
    val onConnectClick: () -> Unit = {
        when (state.vpn) {
            is VpnState.Connected, VpnState.Connecting -> vm.disconnect()
            else -> if (vm.canConnect()) {
                val intent = VpnService.prepare(context)
                if (intent != null) vpnPermission.launch(intent) else vm.connect()
            }
        }
    }

    val importPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let { vm.import(it) } }

    LaunchedEffect(tab) {
        if (tab == Tab.SERVERS) vm.refreshServers()
    }

    // Keep server online status fresh while the app is on screen
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                delay(60_000)
                vm.refreshServers()
            }
        }
    }

    BackHandler(enabled = !showSplash && tab != Tab.HOME) { tab = Tab.HOME }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        if (showSplash) {
            SplashScreen(onDone = { showSplash = false })
        } else {
            Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) {
                AnimatedContent(
                    targetState = tab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f),
                    label = "tabs",
                ) { current ->
                    when (current) {
                        Tab.HOME -> HomeScreen(
                            state = state,
                            onConnectClick = onConnectClick,
                            onOpenServers = { tab = Tab.SERVERS },
                            onOpenPremium = { tab = Tab.PREMIUM },
                            onOpenSettings = { tab = Tab.SETTINGS },
                        )
                        Tab.SERVERS -> ServersScreen(
                            state = state,
                            onPick = { vm.select(it); tab = Tab.HOME },
                            onToggleFavorite = { vm.toggleFavorite(it) },
                            onDelete = { vm.delete(it) },
                            onImport = { importPicker.launch(arrayOf("*/*")) },
                            onRefresh = { vm.refreshServersNow() },
                        )
                        Tab.PREMIUM -> PremiumScreen(
                            onBack = { tab = Tab.HOME },
                            onSubscribe = { vm.message("Subscriptions arrive in a later version. Everything is free for now.") },
                        )
                        Tab.SETTINGS -> SettingsScreen(
                            state = state,
                            onImport = { vm.import(it) },
                            onAutoConnect = { vm.setAutoConnect(it) },
                            onBlockAds = { vm.setBlockAds(it) },
                            onTheme = { vm.setTheme(it) },
                            onOpenPremium = { tab = Tab.PREMIUM },
                            onDeleteData = if (vm.hasApi) ({ vm.deleteMyData() }) else null,
                            apiUrl = vm.apiUrl,
                            onApiUrl = { vm.setApiUrl(it) },
                        )
                    }
                }
                PillNavBar(current = tab, onSelect = { tab = it }, modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp))
            }
        }
        SnackbarHost(
            snackbar,
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.safeDrawing).padding(bottom = 76.dp),
        )
    }
}
