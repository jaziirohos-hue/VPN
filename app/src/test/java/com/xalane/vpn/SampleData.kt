package com.xalane.vpn

import com.xalane.vpn.data.AppSettings
import com.xalane.vpn.data.Server
import com.xalane.vpn.ui.UiState

object SampleData {
    val servers = listOf(
        Server("de-fra", "Germany", "DE", "Frankfurt", 50.1, 8.7, 24, true, streaming = true),
        Server("nl-ams", "Netherlands", "NL", "Amsterdam", 52.4, 4.9, 31, true),
        Server("gb-lon", "United Kingdom", "GB", "London", 51.5, -0.1, 38, true, streaming = true),
        Server("fr-par", "France", "FR", "Paris", 48.85, 2.35, 42, true),
        Server("ke-nbo", "Kenya", "KE", "Nairobi", -1.3, 36.8, 64, true),
        Server("us-nyc", "United States", "US", "New York", 40.7, -74.0, 96, true, streaming = true),
        Server("jp-tyo", "Japan", "JP", "Tokyo", 35.7, 139.7, 212, true),
    )

    fun state(selected: String = "de-fra") = UiState(
        servers = servers,
        settings = AppSettings(selectedServerId = selected, favorites = setOf("de-fra")),
        publicIp = "102.68.77.13",
        ipLoading = false,
    )
}
