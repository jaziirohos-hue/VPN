package com.xalane.vpn.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.AppFlags
import com.xalane.vpn.BuildConfig
import com.xalane.vpn.data.ThemeMode
import com.xalane.vpn.ui.UiState
import com.xalane.vpn.ui.components.SectionLabel
import com.xalane.vpn.ui.theme.Sora
import com.xalane.vpn.ui.theme.X

@Composable
fun SettingsScreen(
    state: UiState,
    onImport: (android.net.Uri) -> Unit,
    onAutoConnect: (Boolean) -> Unit,
    onBlockAds: (Boolean) -> Unit,
    onTheme: (ThemeMode) -> Unit,
    onOpenPremium: () -> Unit,
    onDeleteData: (() -> Unit)? = null,
    apiUrl: String = "",
    onApiUrl: (String?) -> Unit = {},
) {
    val c = X.colors
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onImport) }
    val s = state.settings
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    var editAddress by rememberSaveable { mutableStateOf(false) }
    var address by rememberSaveable(apiUrl) { mutableStateOf(apiUrl) }
    if (editAddress) {
        AlertDialog(
            onDismissRequest = { editAddress = false },
            title = { Text("Xalane server address") },
            text = {
                Column {
                    Text("The address of your Xalane API, for example https://xalane.duckdns.org", color = c.muted, fontSize = 13.sp)
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        singleLine = true,
                        placeholder = { Text("https://") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
            },
            confirmButton = { TextButton(onClick = { onApiUrl(address); editAddress = false }) { Text("Save") } },
            dismissButton = { TextButton(onClick = { onApiUrl(null); editAddress = false }) { Text("Use default") } },
            containerColor = c.surface, titleContentColor = c.text, textContentColor = c.muted,
        )
    }
    if (confirmDelete && onDeleteData != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete your data?") },
            text = { Text("This disconnects the VPN and removes this phone's key and addresses from Xalane servers. You can connect again later.") },
            confirmButton = { TextButton(onClick = { onDeleteData(); confirmDelete = false }) { Text("Delete", color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            containerColor = c.surface, titleContentColor = c.text, textContentColor = c.muted,
        )
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 16.dp)) {
        ScreenTitle("Settings")

        // Plan card
        Row(
            Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Brush.linearGradient(listOf(c.accent.copy(alpha = 0.25f), c.surface)))
                .border(1.dp, c.accent.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(c.raised), contentAlignment = Alignment.Center) {
                Text("X", color = c.accentHi, fontFamily = Sora, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Column(Modifier.weight(1f)) {
                Text("Free plan", color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("No account needed", color = c.muted, fontSize = 12.5.sp)
            }
            if (AppFlags.demoContent) Text(
                "Upgrade",
                color = c.onAccent,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clip(RoundedCornerShape(50)).background(c.accent).clickable(role = Role.Button, onClick = onOpenPremium)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            )
        }

        SectionLabel("Connection", Modifier.padding(top = 10.dp))
        XCard {
            NavItem(Icons.Rounded.Lock, "Protocol", "Fast, modern encryption", "WireGuard", onClick = null)
            Divider()
            NavItem(
                Icons.Rounded.Dns, "Xalane server",
                apiUrl.removePrefix("https://").removePrefix("http://").ifBlank { "Not set" }, null,
            ) { address = apiUrl; editAddress = true }
            Divider()
            NavItem(Icons.Rounded.FileUpload, "Import WireGuard config", "Add a server from a .conf file", null) {
                picker.launch(arrayOf("*/*"))
            }
            Divider()
            ToggleItem(Icons.Rounded.Sync, "Auto-connect", "Connect when the app opens", s.autoConnect, onAutoConnect)
            Divider()
            NavItem(Icons.Rounded.Shield, "Kill switch", "Turn on \"Block connections without VPN\" in Android", null) {
                try {
                    context.startActivity(Intent(Settings.ACTION_VPN_SETTINGS))
                } catch (_: ActivityNotFoundException) {
                    context.startActivity(Intent(Settings.ACTION_SETTINGS))
                }
            }
        }

        SectionLabel("Privacy", Modifier.padding(top = 10.dp))
        XCard {
            ToggleItem(Icons.Rounded.Block, "Block ads and trackers", "Uses AdGuard DNS while connected", s.blockAds, onBlockAds)
            if (onDeleteData != null) {
                Divider()
                NavItem(Icons.Rounded.DeleteForever, "Delete my data", "Removes this phone from Xalane servers", null) { confirmDelete = true }
            }
        }

        SectionLabel("App", Modifier.padding(top = 10.dp))
        XCard {
            val next = when (s.theme) {
                ThemeMode.DARK -> ThemeMode.LIGHT
                ThemeMode.LIGHT -> ThemeMode.SYSTEM
                ThemeMode.SYSTEM -> ThemeMode.DARK
            }
            val label = when (s.theme) {
                ThemeMode.DARK -> "Dark"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.SYSTEM -> "System"
            }
            NavItem(Icons.Rounded.DarkMode, "Theme", "Tap to switch", label) { onTheme(next) }
            Divider()
            NavItem(Icons.Rounded.Info, "About", "Xalane VPN", "v${BuildConfig.VERSION_NAME}", onClick = null)
        }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(color = X.colors.outline.copy(alpha = 0.7f), thickness = 1.dp)
}

@Composable
private fun ItemIcon(icon: ImageVector) {
    val c = X.colors
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(c.accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = c.accentHi, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun NavItem(icon: ImageVector, title: String, subtitle: String?, value: String?, onClick: (() -> Unit)?) {
    val c = X.colors
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            if (subtitle != null) Text(subtitle, color = c.muted, fontSize = 12.5.sp)
        }
        if (value != null) Text(value, color = c.muted, fontSize = 13.5.sp)
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = c.muted)
    }
}

@Composable
private fun ToggleItem(icon: ImageVector, title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val c = X.colors
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Switch) { onChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ItemIcon(icon)
        Column(Modifier.weight(1f)) {
            Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            Text(subtitle, color = c.muted, fontSize = 12.5.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = c.accent, checkedThumbColor = c.onAccent,
                uncheckedTrackColor = c.raised, uncheckedThumbColor = c.muted, uncheckedBorderColor = c.outline,
            ),
        )
    }
}
