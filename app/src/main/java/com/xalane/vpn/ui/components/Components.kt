package com.xalane.vpn.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.data.Server
import com.xalane.vpn.ui.theme.Figtree
import com.xalane.vpn.ui.theme.Mono
import com.xalane.vpn.ui.theme.X

fun flagEmoji(code: String): String =
    if (code.length != 2) "" else code.uppercase().map { String(Character.toChars(0x1F1E6 + (it - 'A'))) }.joinToString("")

/** Round flag for a server. Null means the automatic "fastest server" choice. */
@Composable
fun CountryBadge(server: Server?, size: Dp = 40.dp) {
    val c = X.colors
    val isAuto = server == null
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(if (isAuto) c.accent else c.raised)
            .border(1.5.dp, c.outline, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        when {
            isAuto -> Icon(Icons.Rounded.Bolt, null, tint = c.onAccent, modifier = Modifier.size(size * 0.5f))
            server!!.countryCode.isBlank() -> Icon(Icons.Rounded.VpnKey, null, tint = c.accentHi, modifier = Modifier.size(size * 0.45f))
            else -> {
                val fs = with(LocalDensity.current) { (size * 0.78f).toSp() }
                Text(flagEmoji(server.countryCode), fontSize = fs)
            }
        }
    }
}

@Composable
fun DemoChip(text: String = "DEMO") {
    val c = X.colors
    Text(
        text,
        color = c.warn,
        fontFamily = Figtree,
        fontWeight = FontWeight.Bold,
        fontSize = 9.5.sp,
        letterSpacing = 1.sp,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(c.warn.copy(alpha = 0.12f))
            .border(1.dp, c.warn.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}

/** Latency as color plus bar count, so it reads without relying on color alone. */
@Composable
fun LatencyIndicator(ms: Int?) {
    val c = X.colors
    if (ms == null) {
        Text("— ms", color = c.faint, fontFamily = Mono, fontSize = 12.5.sp)
        return
    }
    val color = when {
        ms < 60 -> c.ok
        ms < 130 -> c.warn
        else -> c.bad
    }
    val bars = when {
        ms < 40 -> 4
        ms < 60 -> 3
        ms < 130 -> 2
        else -> 1
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            listOf(4, 7, 10, 12).forEachIndexed { i, h ->
                Box(Modifier.width(3.dp).height(h.dp).clip(RoundedCornerShape(1.dp)).background(if (i < bars) color else c.raised))
            }
        }
        Text("$ms ms", color = color, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}

enum class Tab(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Rounded.Home),
    SERVERS("Servers", Icons.Rounded.Language),
    PREMIUM("Premium", Icons.Rounded.Star),
    SETTINGS("Settings", Icons.Rounded.Settings),
}

/** Floating pill navigation. The selected tab expands to show its label. */
@Composable
fun PillNavBar(current: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val c = X.colors
    Row(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(32.dp))
            .background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(32.dp))
            .padding(6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tab.entries.forEach { tab ->
            val selected = tab == current
            val bg by animateColorAsState(if (selected) c.accent else c.raised.copy(alpha = 0.7f), label = "navbg")
            Row(
                Modifier
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { contentDescription = tab.label }
                    .padding(horizontal = if (selected) 18.dp else 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(tab.icon, null, tint = if (selected) c.onAccent else c.muted, modifier = Modifier.size(20.dp))
                if (selected) Text(tab.label, color = c.onAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
    }
}

enum class OrbState { OFF, CONNECTING, ON }

/** The large connect button with glow, pulse rings and a connecting spinner. */
@Composable
fun PowerButton(state: OrbState, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = X.colors
    val inf = rememberInfiniteTransition(label = "orb")
    val pulse by inf.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing)), label = "pulse")
    val spin by inf.animateFloat(0f, 360f, infiniteRepeatable(tween(1000, easing = LinearEasing)), label = "spin")
    val breathe by inf.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "breathe")

    Box(modifier.size(210.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2
            if (state == OrbState.ON) {
                drawCircle(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.35f), Color.Transparent), radius = r), r)
                for (k in 0..1) {
                    val p = (pulse + k * 0.5f) % 1f
                    drawCircle(c.glow.copy(alpha = (1f - p) * 0.5f), r * (0.68f + 0.32f * p), style = Stroke(1.5.dp.toPx()))
                }
            } else {
                drawCircle(c.accent.copy(alpha = 0.16f), r * 0.98f, style = Stroke(1.dp.toPx()))
                drawCircle(c.accent.copy(alpha = 0.10f), r * 0.86f, style = Stroke(1.dp.toPx()))
            }
        }
        val on = state == OrbState.ON
        Box(
            Modifier
                .size(144.dp)
                .scale(if (on) breathe else 1f)
                .clip(CircleShape)
                .background(
                    if (on) Brush.radialGradient(listOf(c.orbLight, c.accent, c.orbDeep), center = Offset.Unspecified)
                    else Brush.radialGradient(listOf(c.raised, c.surface)),
                )
                .border(BorderStroke(2.dp, if (on) Color.White.copy(alpha = 0.25f) else c.outline), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Button,
                    onClick = onClick,
                )
                .semantics { contentDescription = label },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.PowerSettingsNew, null,
                tint = if (on) c.onAccent else c.accentHi,
                modifier = Modifier.size(56.dp),
            )
        }
        if (state == OrbState.CONNECTING) {
            Canvas(Modifier.size(152.dp).rotate(spin)) {
                drawArc(
                    Brush.sweepGradient(listOf(Color.Transparent, c.accent, c.glow)),
                    0f, 270f, false, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, trailing: String? = null) {
    val c = X.colors
    Row(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp)) {
        Text(text.uppercase(), color = c.faint, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 1.2.sp)
        Spacer(Modifier.weight(1f))
        if (trailing != null) Text(trailing, color = c.faint, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}
