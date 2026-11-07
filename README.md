# Xalane VPN for Android

Version 1 of the Xalane VPN app: Kotlin, Jetpack Compose and the official WireGuard engine.

| Home | Connected | Servers | Settings | Premium |
|---|---|---|---|---|
| ![](docs/screenshots/2_home_disconnected.png) | ![](docs/screenshots/3_home_connected.png) | ![](docs/screenshots/5_servers.png) | ![](docs/screenshots/6_settings.png) | ![](docs/screenshots/7_premium.png) |

## What works in v1

- **Real VPN connections** using WireGuard (`com.wireguard.android:tunnel`). Android shows its standard VPN permission prompt the first time.
- **Import your own server**: Settings → *Import WireGuard config* accepts any standard `.conf` file. Imported servers appear under *Your servers* and connect for real.
- **Home**: connect button, session timer, live download and upload speed, and your public IP (before and after connecting).
- **Servers**: search, filters (Fastest, Favorites, Streaming), favorites, and an automatic *Fastest server* option.
- **Settings**: auto-connect, ad and tracker blocking (AdGuard DNS while connected), dark, light or system theme, and a shortcut to Android's kill switch (*Block connections without VPN*).
- **Splash** and **Premium** screens matching the approved design.

## What is still Demo

- The 13 built-in locations in `app/src/main/assets/servers.json` are **Demo**: they show sample latency and cannot connect yet. They are labeled Demo in the app. A location becomes live as soon as its entry gets a `wireguardConfig` (full config text), which the Xalane backend will provide later.
- **Premium** prices are samples. Subscriptions (Google Play Billing) are not connected yet.

## Build

Requirements: JDK 17+ and the Android SDK (platform 35).

```bash
./gradlew assembleDebug
```

The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

The release build (`./gradlew assembleRelease`) is currently signed with the debug key so it installs for testing. Create your own signing key before publishing to Google Play.

## Tests

```bash
./gradlew testDebugUnitTest
```

This runs logic tests plus screenshot tests (Robolectric + Roborazzi) that render every screen to `app/build/outputs/roborazzi/`. If Maven Central rate-limits downloads, set `ROBOLECTRIC_REPO_URL` to a Maven Central mirror.

## Project layout

```
app/src/main/java/com/xalane/vpn/
  data/        servers (bundled + imported configs), settings (DataStore)
  vpn/         WireGuard tunnel manager, public IP lookup
  ui/          view model, app root, theme, components, screens
docs/          approved design preview and screenshots
```

## Next steps

1. Backend: account and device registration, server list API, issuing WireGuard configs per device.
2. VPN servers: deploy WireGuard servers in the chosen countries.
3. Google Play Billing for Premium.

Fonts: Sora, Figtree and JetBrains Mono, under the SIL Open Font License.
