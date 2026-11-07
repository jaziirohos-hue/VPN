package com.xalane.vpn.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.BuildConfig
import com.xalane.vpn.R
import com.xalane.vpn.ui.theme.Sora
import com.xalane.vpn.ui.theme.X
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: () -> Unit) {
    val c = X.colors
    LaunchedEffect(Unit) {
        delay(1500)
        onDone()
    }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(c.accent.copy(alpha = 0.28f), c.bg), radius = 900f)),
    ) {
        Column(Modifier.align(Alignment.Center).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Image(painterResource(R.drawable.ic_logo), contentDescription = null, modifier = Modifier.size(132.dp))
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Xalane", color = c.text, fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 38.sp)
                Text("VPN", color = c.accentHi, fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 38.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text("Fast, private connections with one tap.", color = c.muted, fontSize = 15.sp, textAlign = TextAlign.Center)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            LinearProgressIndicator(
                modifier = Modifier.width(120.dp).height(3.dp).clip(RoundedCornerShape(2.dp)),
                color = c.accent,
                trackColor = c.raised,
            )
            Spacer(Modifier.height(12.dp))
            Text("v${BuildConfig.VERSION_NAME}", color = c.faint, fontSize = 12.sp)
        }
    }
}
