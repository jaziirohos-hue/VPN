package com.xalane.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.ui.theme.Sora
import com.xalane.vpn.ui.theme.X

@Composable
fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = X.colors
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = c.text, modifier = Modifier.size(20.dp)) }
}

@Composable
fun ScreenTitle(text: String, trailing: @Composable (() -> Unit)? = null) {
    val c = X.colors
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = c.text, fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun XCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val c = X.colors
    Column(
        modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(c.surface)
            .border(1.dp, c.outline, RoundedCornerShape(20.dp)),
        content = content,
    )
}
