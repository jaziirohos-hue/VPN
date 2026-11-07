package com.xalane.vpn.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.xalane.vpn.ui.theme.X

private const val TOP_LAT = 85.0
private const val LAT_SPAN = 145.0

// Rough continent outlines as (lon, lat) pairs. Only used for the decorative dot map.
private val CONTINENTS: List<List<Pair<Double, Double>>> = listOf(
    "-165,65 -140,70 -95,72 -80,65 -60,55 -55,48 -70,43 -80,32 -81,25 -97,26 -97,18 -87,15 -78,8 -83,8 -92,14 -105,20 -115,30 -125,40 -125,50 -135,58 -150,60 -165,60",
    "-55,60 -45,60 -20,70 -20,82 -60,82 -70,76",
    "-78,8 -60,10 -50,0 -35,-5 -40,-22 -48,-28 -58,-38 -65,-55 -72,-50 -72,-30 -80,-5",
    "-10,36 0,43 -5,48 -2,50 5,54 10,58 5,62 15,70 30,71 40,66 40,45 28,40 20,40 12,44 10,38 -5,36",
    "-6,50 2,51 -2,59 -6,57",
    "-17,15 -17,21 -10,30 -5,36 10,37 20,32 32,31 35,28 43,12 51,12 42,-2 40,-15 35,-25 20,-35 17,-30 12,-17 13,-5 9,4 -8,4 -15,10",
    "44,-13 50,-15 47,-25 43,-22",
    "40,45 40,66 60,70 80,73 110,77 140,72 180,68 180,62 160,60 155,50 140,45 130,35 122,30 120,22 108,20 108,12 100,10 98,16 90,22 80,15 77,8 72,20 60,25 50,30 57,20 52,15 43,12 35,28 35,36 28,40",
    "98,16 103,1 104,10 108,12",
    "95,5 106,-6 120,-9 140,-8 140,-2 120,2 110,2 100,2",
    "130,32 142,36 145,44 140,42 135,35",
    "114,-22 122,-18 130,-12 137,-12 142,-11 146,-19 153,-25 151,-34 146,-39 138,-35 132,-32 115,-34",
    "172,-35 178,-38 172,-46 167,-46",
).map { s -> s.split(' ').map { p -> p.split(',').let { it[0].toDouble() to it[1].toDouble() } } }

private fun inside(lon: Double, lat: Double, poly: List<Pair<Double, Double>>): Boolean {
    var inside = false
    var j = poly.lastIndex
    for (i in poly.indices) {
        val (xi, yi) = poly[i]
        val (xj, yj) = poly[j]
        if ((yi > lat) != (yj > lat) && lon < (xj - xi) * (lat - yi) / (yj - yi) + xi) inside = !inside
        j = i
    }
    return inside
}

private val DOTS: List<Pair<Double, Double>> by lazy {
    val step = 3.4
    buildList {
        var lat = TOP_LAT - step / 2
        while (lat > TOP_LAT - LAT_SPAN) {
            var lon = -180 + step / 2
            while (lon < 180) {
                if (CONTINENTS.any { inside(lon, lat, it) }) add(lon to lat)
                lon += step
            }
            lat -= step
        }
    }
}

/**
 * Dotted world map. Shows a pulsing pin at [pin] (lat, lon) when [showPin] is true.
 * Pass drawDots = false to draw only the pin, as a layer above other content.
 */
@Composable
fun WorldMap(pin: Pair<Double, Double>?, showPin: Boolean, modifier: Modifier = Modifier, drawDots: Boolean = true) {
    val c = X.colors
    val dots = remember { DOTS }
    val p by rememberInfiniteTransition(label = "pin").animateFloat(
        0f, 1f, infiniteRepeatable(tween(2000, easing = LinearEasing)), label = "pinpulse",
    )
    Canvas(modifier) {
        fun pos(lat: Double, lon: Double) = Offset(
            ((lon + 180) / 360 * size.width).toFloat(),
            ((TOP_LAT - lat) / LAT_SPAN * size.height).toFloat(),
        )
        val r = size.width / (360 / 3.4f) * 0.3f
        if (drawDots) dots.forEach { (lon, lat) -> drawCircle(c.mapDot, r, pos(lat, lon)) }
        if (showPin && pin != null) {
            val o = pos(pin.first, pin.second)
            drawCircle(c.accent.copy(alpha = 0.25f), 12.dp.toPx(), o)
            drawCircle(c.glow.copy(alpha = 1f - p), 6.dp.toPx() + 14.dp.toPx() * p, o, style = Stroke(1.5.dp.toPx()))
            drawCircle(c.glow, 4.5.dp.toPx(), o)
        }
    }
}
