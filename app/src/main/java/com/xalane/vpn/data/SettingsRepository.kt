package com.xalane.vpn.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val theme: ThemeMode = ThemeMode.DARK,
    val autoConnect: Boolean = false,
    val blockAds: Boolean = false,
    val selectedServerId: String = AUTO_SERVER_ID,
    val favorites: Set<String> = emptySet(),
    /** Server address the user typed in Settings; null means the built-in default. */
    val apiUrl: String? = null,
)

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val theme = stringPreferencesKey("theme")
        val autoConnect = booleanPreferencesKey("auto_connect")
        val blockAds = booleanPreferencesKey("block_ads")
        val selected = stringPreferencesKey("selected_server")
        val favorites = stringSetPreferencesKey("favorites")
        val apiUrl = stringPreferencesKey("api_url")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { p ->
        AppSettings(
            theme = p[Keys.theme]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.DARK,
            autoConnect = p[Keys.autoConnect] ?: false,
            blockAds = p[Keys.blockAds] ?: false,
            selectedServerId = p[Keys.selected] ?: AUTO_SERVER_ID,
            favorites = p[Keys.favorites] ?: emptySet(),
            apiUrl = p[Keys.apiUrl],
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        context.dataStore.edit { block(it) }
    }

    suspend fun setTheme(mode: ThemeMode) = edit { it[Keys.theme] = mode.name }
    suspend fun setAutoConnect(on: Boolean) = edit { it[Keys.autoConnect] = on }
    suspend fun setBlockAds(on: Boolean) = edit { it[Keys.blockAds] = on }
    suspend fun setApiUrl(url: String?) = edit { if (url.isNullOrBlank()) it.remove(Keys.apiUrl) else it[Keys.apiUrl] = url }
    suspend fun select(id: String) = edit { it[Keys.selected] = id }
    suspend fun toggleFavorite(id: String) = edit { p ->
        val current = p[Keys.favorites] ?: emptySet()
        p[Keys.favorites] = if (id in current) current - id else current + id
    }

    @Suppress("unused")
    private fun <T> Preferences.getOr(key: Preferences.Key<T>, default: T): T = this[key] ?: default
}
