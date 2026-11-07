package com.xalane.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.ui.components.DemoChip
import com.xalane.vpn.ui.theme.Mono
import com.xalane.vpn.ui.theme.Sora
import com.xalane.vpn.ui.theme.X

private data class Plan(val id: String, val name: String, val detail: String, val price: String, val tag: String? = null)

private val PLANS = listOf(
    Plan("m1", "1 Month", "1 device", "$4.99"),
    Plan("m3", "3 Months", "Billed $11.97 · 3 devices", "$3.99"),
    Plan("y1", "1 Year", "Billed $29.88 · 5 devices", "$2.49", "Save 50%"),
)

@Composable
fun PremiumScreen(onBack: () -> Unit, onSubscribe: () -> Unit) {
    val c = X.colors
    var plan by rememberSaveable { mutableStateOf("y1") }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            CircleIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onBack)
            Spacer(Modifier.weight(1f))
            DemoChip("SAMPLE PRICES")
        }
        Box(
            Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(c.warn.copy(alpha = 0.12f))
                .border(1.dp, c.warn.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.Star, null, tint = c.warn, modifier = Modifier.size(30.dp)) }
        Text("Upgrade to Premium", color = c.text, fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, modifier = Modifier.padding(top = 14.dp))
        Text(
            "Unlock every location, faster speeds and an ad-free app.",
            color = c.muted, fontSize = 14.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp, vertical = 6.dp),
        )

        Column(Modifier.padding(horizontal = 20.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Feature(Icons.Rounded.Shield, "Hide your IP", "Browse anonymously", Modifier.weight(1f))
                Feature(Icons.Rounded.Bolt, "Faster speeds", "Up to 1 Gbps servers", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Feature(Icons.Rounded.Block, "No ads", "A clean, quiet app", Modifier.weight(1f))
                Feature(Icons.Rounded.Language, "All locations", "Every Xalane country", Modifier.weight(1f))
            }
        }

        Column(Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PLANS.forEach { p ->
                val on = p.id == plan
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (on) c.accent.copy(alpha = 0.12f) else c.surface)
                        .border(1.5.dp, if (on) c.accent else c.outline, RoundedCornerShape(18.dp))
                        .clickable(role = Role.RadioButton) { plan = p.id }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(p.name, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            if (p.tag != null) {
                                Text(
                                    p.tag.uppercase(), color = c.onAccent, fontWeight = FontWeight.Bold, fontSize = 9.5.sp,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(c.ok).padding(horizontal = 7.dp, vertical = 2.dp),
                                )
                            }
                        }
                        Text(p.detail, color = c.muted, fontSize = 12.5.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(p.price, color = c.text, fontFamily = Mono, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Text("/month", color = c.muted, fontSize = 11.sp)
                    }
                    Box(
                        Modifier.size(20.dp).clip(CircleShape).border(2.dp, if (on) c.accent else c.outline, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Box(Modifier.size(10.dp).clip(CircleShape).background(c.accent)) }
                }
            }
        }

        Box(
            Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth()
                .height(54.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(Brush.verticalGradient(listOf(c.orbLight, c.accent)))
                .clickable(role = Role.Button, onClick = onSubscribe),
            contentAlignment = Alignment.Center,
        ) { Text("Subscribe to Premium", color = c.onAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp) }
        Text("Subscriptions are not live in this version. Prices are samples.", color = c.faint, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 24.dp))
    }
}

@Composable
private fun Feature(icon: ImageVector, title: String, subtitle: String, modifier: Modifier) {
    val c = X.colors
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = c.accentHi, modifier = Modifier.size(20.dp))
        Column {
            Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp)
            Text(subtitle, color = c.muted, fontSize = 12.sp)
        }
    }
}
