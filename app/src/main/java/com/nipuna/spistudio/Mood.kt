package com.nipuna.spistudio

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp as lerpColor
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class Kind { LEAF, PETAL, AURORA, WAVES, SUNSET, STARS, RAIN, SNOW, FIREFLY }

class Mood(
    val name: String,
    val top: Color,
    val bottom: Color,
    val glow: Color,
    val kind: Kind,
    val c1: Color,
    val c2: Color
)

val Moods = listOf(
    Mood("Forest Morning", Color(0xFF021A12), Color(0xFF0B4A2E), Color(0xFF7CFFB0), Kind.LEAF, Color(0xFF5DDB7B), Color(0xFFB6F36A)),
    Mood("Sakura Night", Color(0xFF14061C), Color(0xFF4A1A50), Color(0xFFFF8FD0), Kind.PETAL, Color(0xFFFFB7DD), Color(0xFFFF7FBF)),
    Mood("Aurora", Color(0xFF020B1A), Color(0xFF0A2A3A), Color(0xFF66FFD1), Kind.AURORA, Color(0xFF3CFFB0), Color(0xFF9B6BFF)),
    Mood("Deep Ocean", Color(0xFF01101F), Color(0xFF05406B), Color(0xFF4FC3FF), Kind.WAVES, Color(0xFF2E9BFF), Color(0xFF63E6FF)),
    Mood("Sunset", Color(0xFF1A0A24), Color(0xFF7A2A3A), Color(0xFFFFA45C), Kind.SUNSET, Color(0xFFFFC27A), Color(0xFFFF7A9A)),
    Mood("Starry Night", Color(0xFF000308), Color(0xFF0A1030), Color(0xFF8FA8FF), Kind.STARS, Color(0xFFFFFFFF), Color(0xFFBBD0FF)),
    Mood("Autumn", Color(0xFF1C0C05), Color(0xFF5A2410), Color(0xFFFF9A4A), Kind.LEAF, Color(0xFFFF8A3C), Color(0xFFE0452A)),
    Mood("Rainy", Color(0xFF05080F), Color(0xFF1B2A3E), Color(0xFF7FA8D8), Kind.RAIN, Color(0xFFA8C8F0), Color(0xFF7FA8D8)),
    Mood("Snowfall", Color(0xFF060B14), Color(0xFF1D3350), Color(0xFFBFE0FF), Kind.SNOW, Color(0xFFFFFFFF), Color(0xFFD8ECFF)),
    Mood("Fireflies", Color(0xFF010805), Color(0xFF082818), Color(0xFFE6FF70), Kind.FIREFLY, Color(0xFFF0FF8A), Color(0xFFB6FF5A))
)

private const val MOOD_SECONDS = 60f
private const val FADE_SECONDS = 6f

private fun rnd(i: Int, k: Int): Float {
    var x = i * 374761393 + k * 668265263
    x = (x xor (x ushr 13)) * 1274126177
    x = x xor (x ushr 16)
    return (x and 0xFFFF) / 65535f
}

private fun smooth(x: Float): Float = x * x * (3f - 2f * x)

/** Living background: 10 nature moods, one per minute, cross-faded slowly. */
@Composable
fun MoodBackground(skipSignal: Int, onName: (String) -> Unit, modifier: Modifier = Modifier) {
    val clock = remember { mutableFloatStateOf(0f) }
    val offset = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) {
            withFrameNanos { clock.floatValue = (it - start) / 1_000_000_000f }
        }
    }
    LaunchedEffect(skipSignal) {
        if (skipSignal > 0) {
            val cyc = (clock.floatValue + offset.floatValue) % MOOD_SECONDS
            offset.floatValue += ((MOOD_SECONDS - FADE_SECONDS - cyc) + MOOD_SECONDS) % MOOD_SECONDS
        }
    }
    val idx by remember {
        derivedStateOf { ((clock.floatValue + offset.floatValue) / MOOD_SECONDS).toInt() % Moods.size }
    }
    LaunchedEffect(idx) { onName(Moods[idx].name) }

    Canvas(modifier.fillMaxSize()) {
        val tt = clock.floatValue + offset.floatValue
        val cyc = tt % MOOD_SECONDS
        val i = (tt / MOOD_SECONDS).toInt() % Moods.size
        val cur = Moods[i]
        val nxt = Moods[(i + 1) % Moods.size]
        val blend = smooth(((cyc - (MOOD_SECONDS - FADE_SECONDS)) / FADE_SECONDS).coerceIn(0f, 1f))
        val top = lerpColor(cur.top, nxt.top, blend)
        val bottom = lerpColor(cur.bottom, nxt.bottom, blend)
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        drawMood(cur, tt, 1f - blend)
        if (blend > 0.001f) drawMood(nxt, tt, blend)
    }
}

private fun DrawScope.drawMood(m: Mood, t: Float, a: Float) {
    val c = Offset(size.width * 0.82f, size.height * 0.10f)
    val r = size.width * 0.95f
    drawCircle(
        brush = Brush.radialGradient(listOf(m.glow.copy(alpha = 0.24f * a), Color.Transparent), center = c, radius = r),
        radius = r,
        center = c
    )
    when (m.kind) {
        Kind.LEAF -> { treeLine(t, m, a); leaves(t, m, a, 26) }
        Kind.PETAL -> petals(t, m, a)
        Kind.AURORA -> { stars(t, m, a, 40); aurora(t, m, a) }
        Kind.WAVES -> { waves(t, m, a); bubbles(t, m, a) }
        Kind.SUNSET -> sunset(t, m, a)
        Kind.STARS -> { stars(t, m, a, 110); shooting(t, m, a) }
        Kind.RAIN -> rain(t, m, a)
        Kind.SNOW -> snow(t, m, a)
        Kind.FIREFLY -> { treeLine(t, m, a); fireflies(t, m, a) }
    }
}

private fun leafPath(x: Float, y: Float, s: Float): Path = Path().apply {
    moveTo(x, y - s)
    quadraticBezierTo(x + s * 0.85f, y - s * 0.2f, x, y + s)
    quadraticBezierTo(x - s * 0.85f, y - s * 0.2f, x, y - s)
    close()
}

private fun DrawScope.leaves(t: Float, m: Mood, a: Float, n: Int) {
    for (i in 0 until n) {
        val sp = 0.02f + rnd(i, 1) * 0.035f
        val ph = rnd(i, 2) * 6.28f
        val prog = (rnd(i, 3) + t * sp) % 1f
        val y = prog * (size.height + 80f) - 40f
        val x = rnd(i, 4) * size.width + sin(t * (0.5f + rnd(i, 5)) + ph) * 40f
        val rot = sin(t * (0.6f + rnd(i, 6)) + ph) * 55f
        val s = 9f + rnd(i, 8) * 15f
        val col = if (rnd(i, 9) > 0.5f) m.c1 else m.c2
        val fade = min(1f, min(prog / 0.08f, (1f - prog) / 0.08f))
        val alpha = 0.6f * a * fade
        rotate(rot, Offset(x, y)) {
            drawPath(leafPath(x, y, s), col.copy(alpha = alpha))
            drawLine(col.copy(alpha = alpha * 0.6f), Offset(x, y - s), Offset(x, y + s), strokeWidth = 1.2f)
        }
    }
}

private fun DrawScope.treeLine(t: Float, m: Mood, a: Float) {
    val dark = lerpColor(m.bottom, Color.Black, 0.65f).copy(alpha = 0.85f * a)
    val h = size.height
    val w = size.width
    for (j in 0 until 6) {
        val x = (j + 0.5f) / 6f * w + (rnd(j, 11) - 0.5f) * 40f
        val th = h * (0.20f + rnd(j, 12) * 0.12f)
        drawRect(dark, Offset(x - 5f, h - th * 0.55f), Size(10f, th * 0.55f))
        val sway = sin(t * 0.7f + j * 1.3f) * 7f
        val rad = w * (0.055f + rnd(j, 13) * 0.03f)
        drawCircle(dark, rad, Offset(x + sway, h - th * 0.62f))
        drawCircle(dark, rad * 0.8f, Offset(x - rad * 0.7f + sway * 1.3f, h - th * 0.50f))
        drawCircle(dark, rad * 0.8f, Offset(x + rad * 0.7f + sway * 0.8f, h - th * 0.52f))
    }
}

private fun DrawScope.petals(t: Float, m: Mood, a: Float) {
    for (i in 0 until 30) {
        val sp = 0.015f + rnd(i, 1) * 0.03f
        val ph = rnd(i, 2) * 6.28f
        val prog = (rnd(i, 3) + t * sp) % 1f
        val y = prog * (size.height + 60f) - 30f
        val x = rnd(i, 4) * size.width + sin(t * 0.7f + ph) * 55f
        val rot = t * (12f + rnd(i, 6) * 25f) + ph * 50f
        val s = 5f + rnd(i, 8) * 8f
        val col = if (rnd(i, 9) > 0.5f) m.c1 else m.c2
        val fade = min(1f, min(prog / 0.08f, (1f - prog) / 0.08f))
        rotate(rot, Offset(x, y)) {
            drawOval(col.copy(alpha = 0.7f * a * fade), Offset(x - s * 1.5f, y - s * 0.8f), Size(s * 3f, s * 1.6f))
        }
    }
}

private fun DrawScope.aurora(t: Float, m: Mood, a: Float) {
    for (b in 0 until 3) {
        val baseY = size.height * (0.20f + 0.12f * b)
        val col = if (b % 2 == 0) m.c1 else m.c2
        val p = Path()
        p.moveTo(0f, baseY)
        var x = 0f
        while (x <= size.width + 16f) {
            val y = baseY + sin(x / size.width * 6f + t * 0.35f + b * 1.7f) * size.height * 0.05f +
                sin(x / size.width * 11f - t * 0.5f) * size.height * 0.02f
            p.lineTo(x, y)
            x += 16f
        }
        p.lineTo(size.width, baseY + size.height * 0.32f)
        p.lineTo(0f, baseY + size.height * 0.32f)
        p.close()
        drawPath(
            p,
            Brush.verticalGradient(
                listOf(col.copy(alpha = 0f), col.copy(alpha = 0.30f * a), col.copy(alpha = 0f)),
                startY = baseY - size.height * 0.06f,
                endY = baseY + size.height * 0.32f
            )
        )
    }
}

private fun DrawScope.waves(t: Float, m: Mood, a: Float) {
    for (k in 0 until 4) {
        val base = size.height * (0.64f + 0.08f * k)
        val amp = size.height * (0.012f + 0.004f * k)
        val col = if (k % 2 == 0) m.c1 else m.c2
        val p = Path()
        p.moveTo(0f, size.height)
        var x = 0f
        while (x <= size.width + 12f) {
            p.lineTo(x, base + sin(x / size.width * (4f + k) + t * (0.5f + 0.15f * k) + k) * amp)
            x += 12f
        }
        p.lineTo(size.width, size.height)
        p.close()
        drawPath(p, col.copy(alpha = (0.10f + 0.04f * k) * a))
    }
}

private fun DrawScope.bubbles(t: Float, m: Mood, a: Float) {
    for (i in 0 until 22) {
        val sp = 0.03f + rnd(i, 1) * 0.04f
        val prog = (rnd(i, 3) + t * sp) % 1f
        val y = size.height * (1f - prog)
        val x = rnd(i, 4) * size.width + sin(t * 0.8f + rnd(i, 2) * 6.28f) * 14f
        val r = 3f + rnd(i, 8) * 9f
        val fade = min(1f, min(prog / 0.1f, (1f - prog) / 0.15f))
        drawCircle(m.c2.copy(alpha = 0.5f * a * fade), r, Offset(x, y), style = Stroke(1.4f))
        drawCircle(Color.White.copy(alpha = 0.35f * a * fade), r * 0.22f, Offset(x - r * 0.4f, y - r * 0.4f))
    }
}

private fun DrawScope.sunset(t: Float, m: Mood, a: Float) {
    val c = Offset(size.width * 0.5f, size.height * 0.78f)
    val r = size.width * 0.9f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(m.glow.copy(alpha = 0.55f * a), m.c2.copy(alpha = 0.20f * a), Color.Transparent),
            center = c, radius = r
        ),
        radius = r, center = c
    )
    for (i in 0 until 5) {
        val x = ((rnd(i, 1) + t * (0.004f + 0.003f * rnd(i, 2))) % 1.3f - 0.15f) * size.width
        val y = size.height * (0.18f + 0.5f * rnd(i, 3))
        drawOval(m.glow.copy(alpha = 0.10f * a), Offset(x, y), Size(size.width * 0.45f, size.height * 0.035f))
    }
    for (i in 0 until 24) {
        val prog = (rnd(i, 3) + t * (0.01f + rnd(i, 1) * 0.02f)) % 1f
        val x = rnd(i, 4) * size.width + sin(t * 0.5f + rnd(i, 2) * 6.28f) * 20f
        val y = size.height * (1f - prog)
        val fade = min(1f, min(prog / 0.1f, (1f - prog) / 0.2f))
        drawCircle(m.c1.copy(alpha = 0.55f * a * fade), 1.5f + rnd(i, 8) * 2f, Offset(x, y))
    }
}

private fun DrawScope.stars(t: Float, m: Mood, a: Float, n: Int) {
    for (i in 0 until n) {
        val tw = 0.35f + 0.65f * (0.5f + 0.5f * sin(t * (0.6f + rnd(i, 3) * 1.4f) + rnd(i, 4) * 6.28f))
        drawCircle(
            m.c1.copy(alpha = 0.85f * a * tw),
            0.8f + rnd(i, 5) * 1.6f,
            Offset(rnd(i, 1) * size.width, rnd(i, 2) * size.height * 0.85f)
        )
    }
}

private fun DrawScope.shooting(t: Float, m: Mood, a: Float) {
    val period = 9f
    val p = (t % period) / period
    if (p < 0.12f) {
        val q = p / 0.12f
        val x0 = size.width * (0.2f + 0.4f * rnd((t / period).toInt(), 21))
        val y0 = size.height * (0.05f + 0.2f * rnd((t / period).toInt(), 22))
        val x = x0 + q * size.width * 0.35f
        val y = y0 + q * size.height * 0.18f
        val alpha = (1f - q) * a
        drawLine(m.c1.copy(alpha = 0.9f * alpha), Offset(x, y), Offset(x - 70f, y - 28f), strokeWidth = 2f)
        drawCircle(Color.White.copy(alpha = alpha), 2.5f, Offset(x, y))
    }
}

private fun DrawScope.rain(t: Float, m: Mood, a: Float) {
    for (i in 0 until 70) {
        val sp = 0.35f + rnd(i, 1) * 0.35f
        val prog = (rnd(i, 3) + t * sp) % 1f
        val x = rnd(i, 4) * size.width
        val y = prog * (size.height + 60f) - 30f
        drawLine(m.c1.copy(alpha = 0.30f * a), Offset(x, y), Offset(x - 4f, y + 22f), strokeWidth = 1.4f)
    }
    for (i in 0 until 7) {
        val ph = (t * 0.5f + rnd(i, 7)) % 1f
        val cx = rnd(i, 8) * size.width
        val cy = size.height * (0.88f + 0.10f * rnd(i, 9))
        val rr = 6f + ph * 34f
        drawOval(m.c2.copy(alpha = 0.40f * a * (1f - ph)), Offset(cx - rr, cy - rr * 0.35f), Size(rr * 2f, rr * 0.7f), style = Stroke(1.3f))
    }
}

private fun DrawScope.snow(t: Float, m: Mood, a: Float) {
    for (i in 0 until 60) {
        val sp = 0.02f + rnd(i, 1) * 0.04f
        val prog = (rnd(i, 3) + t * sp) % 1f
        val y = prog * (size.height + 20f) - 10f
        val x = rnd(i, 4) * size.width + sin(t * 0.6f + rnd(i, 2) * 6.28f) * 25f
        val r = 1.5f + rnd(i, 8) * 3.2f
        val fade = min(1f, min(prog / 0.06f, (1f - prog) / 0.06f))
        drawCircle(m.c1.copy(alpha = 0.75f * a * fade), r, Offset(x, y))
    }
}

private fun DrawScope.fireflies(t: Float, m: Mood, a: Float) {
    for (i in 0 until 28) {
        val ph = rnd(i, 2) * 6.28f
        val x = rnd(i, 4) * size.width + sin(t * (0.25f + rnd(i, 5) * 0.3f) + ph) * 60f
        val y = size.height * (0.25f + 0.6f * rnd(i, 6)) + cos(t * (0.2f + rnd(i, 7) * 0.3f) + ph) * 45f
        val pulse = 0.5f + 0.5f * sin(t * (0.8f + rnd(i, 3)) + ph)
        val r = 2f + rnd(i, 8) * 2.5f
        val pos = Offset(x, y)
        drawCircle(
            brush = Brush.radialGradient(listOf(m.c1.copy(alpha = 0.9f * pulse * a), Color.Transparent), center = pos, radius = r * 6f),
            radius = r * 6f,
            center = pos
        )
        drawCircle(Color.White.copy(alpha = 0.9f * pulse * a), r * 0.6f, pos)
    }
}
