package com.xalane.vpn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xalane.vpn.data.AUTO_SERVER_ID
import com.xalane.vpn.data.Server
import com.xalane.vpn.ui.UiState
import com.xalane.vpn.ui.components.CountryBadge
import com.xalane.vpn.ui.components.DemoChip
import com.xalane.vpn.ui.components.LatencyIndicator
import com.xalane.vpn.ui.components.SectionLabel
import com.xalane.vpn.ui.theme.Figtree
import com.xalane.vpn.ui.theme.X

private enum class Filter(val label: String) { ALL("All"), FASTEST("Fastest"), FAVORITES("Favorites"), STREAMING("Streaming") }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServersScreen(
    state: UiState,
    onPick: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onDelete: (String) -> Unit,
    onImport: () -> Unit = {},
    onRefresh: suspend () -> Unit = {},
) {
    val c = X.colors
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(Filter.ALL) }
    var confirmDelete by rememberSaveable { mutableStateOf<String?>(null) }

    val q = query.trim().lowercase()
    val matches: (Server) -> Boolean = { q.isEmpty() || "${it.country} ${it.city}".lowercase().contains(q) }
    val custom = state.servers.filter { it.custom && matches(it) }
    val locations = state.servers.filter { !it.custom && matches(it) }.let { list ->
        when (filter) {
            Filter.ALL -> list
            Filter.FASTEST -> list.filter { (it.latencyMs ?: Int.MAX_VALUE) < 60 }
            Filter.FAVORITES -> list.filter { it.id in state.settings.favorites }
            Filter.STREAMING -> list.filter { it.streaming }
        }
    }.sortedBy { it.latencyMs ?: Int.MAX_VALUE }
    val selectedId = state.settings.selectedServerId
    val anyDemo = state.servers.any { it.isDemo }

    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                try { onRefresh() } finally { refreshing = false }
            }
        },
        modifier = Modifier.fillMaxSize(),
    ) {
    LazyColumn(Modifier.fillMaxSize()) {
        item {
            ScreenTitle("Locations") { if (anyDemo) DemoChip("DEMO LATENCY") }
            Row(
                Modifier
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(c.surface)
                    .border(1.dp, c.outline, RoundedCornerShape(25.dp))
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Search, null, tint = c.muted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) Text("Search country or city", color = c.faint, fontSize = 15.sp)
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = TextStyle(color = c.text, fontSize = 15.sp, fontFamily = Figtree),
                        cursorBrush = SolidColor(c.accent),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "Clear search", tint = c.muted) }
                }
            }
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Filter.entries.forEach { f ->
                    val on = f == filter
                    Text(
                        f.label,
                        color = if (on) c.text else c.muted,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (on) c.accent.copy(alpha = 0.2f) else c.bg)
                            .border(1.dp, if (on) c.accent.copy(alpha = 0.6f) else c.outline, RoundedCornerShape(10.dp))
                            .clickable(role = Role.Tab) { filter = f }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    )
                }
            }
        }

        if (custom.isNotEmpty()) {
            item { SectionLabel("Your servers", trailing = custom.size.toString()) }
            items(custom, key = { it.id }) { s ->
                ServerRow(s, selected = s.id == selectedId, onClick = { onPick(s.id) }) {
                    IconButton(onClick = { confirmDelete = s.id }) { Icon(Icons.Rounded.DeleteOutline, "Remove ${s.country}", tint = c.muted) }
                }
            }
        }

        if (state.servers.isEmpty()) {
            item { EmptyServers(onImport) }
        }

        if (filter == Filter.ALL && q.isEmpty() && state.servers.isNotEmpty()) {
            item {
                SectionLabel("Recommended")
                val fastest = state.fastest
                ServerRow(
                    server = null,
                    title = "Fastest server",
                    subtitle = "Picks the best location for you",
                    latency = fastest?.latencyMs,
                    selected = selectedId == AUTO_SERVER_ID || state.selected == null,
                    onClick = { onPick(AUTO_SERVER_ID) },
                ) { Spacer(Modifier.width(48.dp)) }
            }
        }

        if (state.servers.any { !it.custom }) item { SectionLabel("All locations", trailing = locations.size.toString()) }
        if (locations.isEmpty() && state.servers.any { !it.custom }) {
            item {
                Text(
                    if (filter == Filter.FAVORITES) "No favorites yet. Tap the star next to a location to add it here." else "No locations match your search.",
                    color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(24.dp),
                )
            }
        }
        items(locations, key = { it.id }) { s ->
            val fav = s.id in state.settings.favorites
            ServerRow(s, selected = s.id == selectedId, onClick = { onPick(s.id) }) {
                IconButton(onClick = { onToggleFavorite(s.id) }) {
                    Icon(
                        if (fav) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        if (fav) "Remove from favorites" else "Add to favorites",
                        tint = if (fav) c.warn else c.faint,
                    )
                }
            }
        }
        if (anyDemo) {
            item {
                Text(
                    "Locations marked Demo aren't live yet. To connect today, import your own WireGuard config in Settings.",
                    color = c.faint, fontSize = 12.5.sp, modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
            }
        }
    }

    }

    confirmDelete?.let { id ->
        val name = state.servers.firstOrNull { it.id == id }?.country ?: "this server"
        AlertDialog(
            onDismissRequest = { confirmDelete = null },
            title = { Text("Remove $name?") },
            text = { Text("The WireGuard config is deleted from this phone.") },
            confirmButton = { TextButton(onClick = { onDelete(id); confirmDelete = null }) { Text("Remove", color = c.bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = null }) { Text("Cancel") } },
            containerColor = c.surface,
            titleContentColor = c.text,
            textContentColor = c.muted,
        )
    }
}

@Composable
private fun ServerRow(
    server: Server?,
    selected: Boolean,
    title: String = server?.country ?: "",
    subtitle: String = server?.city ?: "",
    latency: Int? = server?.latencyMs,
    onClick: () -> Unit,
    trailing: @Composable () -> Unit,
) {
    val c = X.colors
    Row(
        Modifier
            .fillMaxWidth()
            .background(if (selected) c.accent.copy(alpha = 0.12f) else c.bg)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CountryBadge(server, size = 40.dp)
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                if (server?.custom == true) Text("LIVE", color = c.ok, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 1.sp)
                if (server?.remote == true && !server.online) Text("OFFLINE", color = c.faint, fontWeight = FontWeight.Bold, fontSize = 9.5.sp, letterSpacing = 1.sp)
            }
            Text(subtitle, color = c.muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        LatencyIndicator(latency)
        trailing()
    }
}

@Composable
private fun EmptyServers(onImport: () -> Unit) {
    val c = X.colors
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 32.dp)) {
        Text("No servers available", color = c.text, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
        Text(
            "Xalane servers can't be reached right now. Check your internet connection, or add a server by importing a WireGuard config file (.conf).",
            color = c.muted, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 16.dp),
        )
        Text(
            "Import WireGuard config",
            color = c.onAccent, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
            modifier = Modifier.clip(RoundedCornerShape(24.dp)).background(c.accent)
                .clickable(role = Role.Button, onClick = onImport).padding(horizontal = 20.dp, vertical = 12.dp),
        )
    }
}
