package com.xalane.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.ui.UiState
import com.xalane.vpn.ui.components.CountryBadge
import com.xalane.vpn.ui.components.DemoChip
import com.xalane.vpn.ui.components.LatencyIndicator
import com.xalane.vpn.ui.components.OrbState
import com.xalane.vpn.ui.components.PowerButton
import com.xalane.vpn.ui.components.WorldMap
import com.xalane.vpn.ui.theme.Mono
import com.xalane.vpn.ui.theme.Sora
import com.xalane.vpn.ui.theme.X
import com.xalane.vpn.vpn.VpnState
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun HomeScreen(
    state: UiState,
    onConnectClick: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenPremium: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val c = X.colors
    val vpn = state.vpn
    val connected = vpn is VpnState.Connected
    val connecting = vpn is VpnState.Connecting
    val shown = state.connectedServer ?: state.target

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top bar
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.Rounded.Menu, "Settings", onOpenSettings)
            Spacer(Modifier.width(12.dp))
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).background(c.accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Bolt, null, tint = c.onAccent, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(8.dp))
            Text("Xalane", color = c.text, fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.weight(1f))
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(c.warn.copy(alpha = 0.12f))
                    .border(1.dp, c.warn.copy(alpha = 0.4f), RoundedCornerShape(50))
                    .clickable(role = Role.Button, onClick = onOpenPremium)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.Star, null, tint = c.warn, modifier = Modifier.size(16.dp))
                Text("Premium", color = c.warn, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            }
        }

        // Selected server
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(c.surface)
                .border(1.dp, c.outline, RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClick = onOpenServers)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CountryBadge(if (state.isAuto && !connected) null else shown, size = 42.dp)
            Column(Modifier.weight(1f)) {
                Text(
                    if (state.isAuto && !connected) "Fastest server" else shown?.country ?: "Choose a server",
                    color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis,
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val sub = when {
                        connected && state.publicIp != null -> "IP ${state.publicIp}"
                        state.isAuto && !connected -> shown?.let { "Auto · ${it.city}" } ?: "Auto"
                        else -> shown?.city ?: ""
                    }
                    Text(sub, color = c.muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (shown?.isDemo == true) DemoChip()
                }
            }
            LatencyIndicator(shown?.latencyMs)
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
        }

        // Status
        val (statusText, statusColor) = when {
            connected -> "Connection secure" to c.ok
            connecting -> "Connecting…" to c.accentHi
            else -> "Not protected" to c.bad
        }
        Row(
            Modifier.padding(top = 10.dp).clip(RoundedCornerShape(50)).background(statusColor.copy(alpha = 0.13f)).padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(statusColor))
            Text(statusText, color = statusColor, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
        }
        val sub = when {
            connected -> "WireGuard · traffic encrypted"
            connecting -> "Securing your tunnel to ${shown?.city ?: "the server"}"
            state.ipLoading -> "Checking your IP address…"
            state.publicIp != null -> "Your IP ${state.publicIp} is visible"
            else -> "Your IP address is visible"
        }
        Text(sub, color = c.muted, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp, start = 24.dp, end = 24.dp))

        // Timer, map and connect button
        SessionTimer((vpn as? VpnState.Connected)?.sinceMillis)
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(top = 4.dp)) {
            val width = LocalConfiguration.current.screenWidthDp.dp * 1.1f
            val pin = shown?.lat?.let { lat -> shown.lon?.let { lat to it } }
            val mapModifier = Modifier.align(Alignment.TopCenter).offset(y = (-22).dp).requiredWidth(width).aspectRatio(360f / 145f)
            WorldMap(pin = null, showPin = false, modifier = mapModifier)
            PowerButton(
                state = when {
                    connected -> OrbState.ON
                    connecting -> OrbState.CONNECTING
                    else -> OrbState.OFF
                },
                label = if (connected || connecting) "Disconnect" else "Connect",
                onClick = onConnectClick,
            )
            // Pin drawn above the button so it stays visible
            WorldMap(pin = pin, showPin = connected, modifier = mapModifier, drawDots = false)
        }
        Text(
            when {
                connected -> "Tap to disconnect"
                connecting -> "Tap to cancel"
                else -> "Tap to connect"
            },
            color = c.muted, fontSize = 13.sp,
        )

        // Speeds
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SpeedTile(Icons.Rounded.ArrowDownward, "Download", if (connected) state.downBytesPerSec else null, Modifier.weight(1f))
            SpeedTile(Icons.Rounded.ArrowUpward, "Upload", if (connected) state.upBytesPerSec else null, Modifier.weight(1f))
        }
    }
}

@Composable
private fun SessionTimer(sinceMillis: Long?) {
    val c = X.colors
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(sinceMillis) {
        while (sinceMillis != null) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val total = if (sinceMillis == null) 0 else ((now - sinceMillis) / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = total % 3600 / 60
    val s = total % 60
    val text = buildAnnotatedString {
        withStyle(SpanStyle(color = c.faint)) { append(String.format(Locale.US, "%02d:", h)) }
        withStyle(SpanStyle(color = if (sinceMillis == null) c.faint else c.text)) { append(String.format(Locale.US, "%02d:%02d", m, s)) }
    }
    Text(text, fontFamily = Sora, fontWeight = FontWeight.Light, fontSize = 52.sp, modifier = Modifier.padding(top = 12.dp))
}

@Composable
private fun SpeedTile(icon: ImageVector, label: String, bytesPerSec: Long?, modifier: Modifier) {
    val c = X.colors
    Row(
        modifier.clip(RoundedCornerShape(16.dp)).background(c.surface).border(1.dp, c.outline, RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(32.dp).clip(CircleShape).border(1.dp, c.outline, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = c.accentHi, modifier = Modifier.size(16.dp))
        }
        Column {
            Text(label, color = c.muted, fontSize = 11.5.sp)
            val (value, unit) = formatSpeed(bytesPerSec)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = c.text, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(Modifier.width(4.dp))
                Text(unit, color = c.muted, fontSize = 11.sp, modifier = Modifier.padding(bottom = 2.dp))
            }
        }
    }
}

private fun formatSpeed(bytesPerSec: Long?): Pair<String, String> {
    if (bytesPerSec == null) return "—" to "Mbps"
    val bits = bytesPerSec * 8.0
    return if (bits >= 1_000_000) String.format(Locale.US, "%.1f", bits / 1_000_000) to "Mbps"
    else String.format(Locale.US, "%.0f", bits / 1000) to "Kbps"
}
