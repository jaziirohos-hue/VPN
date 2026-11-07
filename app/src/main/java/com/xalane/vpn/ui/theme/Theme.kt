@file:OptIn(ExperimentalTextApi::class)

package com.xalane.vpn.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.xalane.vpn.R
import com.xalane.vpn.data.ThemeMode

@Immutable
data class XColors(
    val bg: Color,
    val surface: Color,
    val raised: Color,
    val outline: Color,
    val accent: Color,
    val accentHi: Color,
    val glow: Color,
    val text: Color,
    val muted: Color,
    val faint: Color,
    val ok: Color,
    val bad: Color,
    val warn: Color,
    val onAccent: Color,
    val mapDot: Color,
    val orbLight: Color,
    val orbDeep: Color,
    val isDark: Boolean,
)

val DarkColors = XColors(
    bg = Color(0xFF0A171D), surface = Color(0xFF132830), raised = Color(0xFF1B3540), outline = Color(0xFF21414C),
    accent = Color(0xFF19D3DA), accentHi = Color(0xFF4FE3E8), glow = Color(0xFF86F3F6),
    text = Color(0xFFE8F7F9), muted = Color(0xFF8AAAB2), faint = Color(0xFF56747D),
    ok = Color(0xFF36D995), bad = Color(0xFFFF5C7A), warn = Color(0xFFFFB547), onAccent = Color(0xFF03272B),
    mapDot = Color(0xFF24505C), orbLight = Color(0xFF9AF6F8), orbDeep = Color(0xFF0A9FAE), isDark = true,
)

val LightColors = XColors(
    bg = Color(0xFFEEF1F5), surface = Color(0xFFFFFFFF), raised = Color(0xFFE4E9EF), outline = Color(0xFFDDE3EA),
    accent = Color(0xFF0A9DAE), accentHi = Color(0xFF0890A0), glow = Color(0xFF3FE0EA),
    text = Color(0xFF0E1726), muted = Color(0xFF667185), faint = Color(0xFFA3ADBC),
    ok = Color(0xFF12A26A), bad = Color(0xFFE04466), warn = Color(0xFFD98E14), onAccent = Color(0xFFFFFFFF),
    mapDot = Color(0xFFC3CCD8), orbLight = Color(0xFF8AF1F5), orbDeep = Color(0xFF0796A7), isDark = false,
)

private fun variable(res: Int, weight: Int) =
    Font(res, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

val Sora = FontFamily(variable(R.font.sora, 300), variable(R.font.sora, 600), variable(R.font.sora, 700))
val Figtree = FontFamily(
    variable(R.font.figtree, 400), variable(R.font.figtree, 500), variable(R.font.figtree, 600), variable(R.font.figtree, 700),
)
val Mono = FontFamily(variable(R.font.jetbrains_mono, 500), variable(R.font.jetbrains_mono, 600))

private val AppTypography = Typography(
    bodyLarge = TextStyle(fontFamily = Figtree, fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontFamily = Figtree, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontFamily = Figtree, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontFamily = Figtree, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
    titleLarge = TextStyle(fontFamily = Sora, fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
)

val LocalXColors = staticCompositionLocalOf { DarkColors }

object X {
    val colors: XColors @Composable get() = LocalXColors.current
}

@Composable
fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}

@Composable
fun XalaneTheme(dark: Boolean, content: @Composable () -> Unit) {
    val c = if (dark) DarkColors else LightColors
    val scheme = if (dark) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.raised, onSurfaceVariant = c.muted,
            outline = c.outline, error = c.bad, inverseSurface = c.text, inverseOnSurface = c.bg,
        )
    } else {
        lightColorScheme(
            primary = c.accent, onPrimary = c.onAccent, background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.raised, onSurfaceVariant = c.muted,
            outline = c.outline, error = c.bad, inverseSurface = c.text, inverseOnSurface = c.bg,
        )
    }
    CompositionLocalProvider(LocalXColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, content = content)
    }
}
