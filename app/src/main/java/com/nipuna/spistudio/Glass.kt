package com.nipuna.spistudio

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class Ico { Folder, Scan, Save, Bolt, Image, Skip }

@Composable
fun GlassIcon(kind: Ico, modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        val s = size.minDimension
        val sw = s * 0.10f
        val st = Stroke(width = sw, cap = StrokeCap.Round, join = StrokeJoin.Round)
        when (kind) {
            Ico.Folder -> {
                val p = Path().apply {
                    moveTo(s * 0.12f, s * 0.30f); lineTo(s * 0.40f, s * 0.30f); lineTo(s * 0.48f, s * 0.40f)
                    lineTo(s * 0.88f, s * 0.40f); lineTo(s * 0.88f, s * 0.78f); lineTo(s * 0.12f, s * 0.78f); close()
                }
                drawPath(p, color, style = st)
            }
            Ico.Scan -> {
                drawCircle(color, radius = s * 0.24f, center = Offset(s * 0.44f, s * 0.44f), style = st)
                drawLine(color, Offset(s * 0.62f, s * 0.62f), Offset(s * 0.84f, s * 0.84f), strokeWidth = sw, cap = StrokeCap.Round)
            }
            Ico.Save -> {
                drawLine(color, Offset(s * 0.5f, s * 0.14f), Offset(s * 0.5f, s * 0.58f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color, Offset(s * 0.31f, s * 0.41f), Offset(s * 0.5f, s * 0.60f), strokeWidth = sw, cap = StrokeCap.Round)
                drawLine(color, Offset(s * 0.69f, s * 0.41f), Offset(s * 0.5f, s * 0.60f), strokeWidth = sw, cap = StrokeCap.Round)
                val p = Path().apply {
                    moveTo(s * 0.18f, s * 0.64f); lineTo(s * 0.18f, s * 0.84f); lineTo(s * 0.82f, s * 0.84f); lineTo(s * 0.82f, s * 0.64f)
                }
                drawPath(p, color, style = st)
            }
            Ico.Bolt -> {
                val k = s / 100f
                val pts = listOf(60f to 4f, 22f to 56f, 46f to 56f, 38f to 96f, 78f to 40f, 54f to 40f, 66f to 4f)
                val p = Path()
                pts.forEachIndexed { i, pt -> if (i == 0) p.moveTo(pt.first * k, pt.second * k) else p.lineTo(pt.first * k, pt.second * k) }
                p.close()
                drawPath(p, color)
                drawPath(p, color, style = Stroke(width = sw * 0.6f, join = StrokeJoin.Round))
            }
            Ico.Image -> {
                drawRoundRect(color, Offset(s * 0.14f, s * 0.20f), Size(s * 0.72f, s * 0.60f), CornerRadius(s * 0.08f), style = st)
                drawCircle(color, radius = s * 0.07f, center = Offset(s * 0.34f, s * 0.40f))
                val p = Path().apply {
                    moveTo(s * 0.18f, s * 0.74f); lineTo(s * 0.42f, s * 0.52f); lineTo(s * 0.58f, s * 0.68f)
                    lineTo(s * 0.68f, s * 0.58f); lineTo(s * 0.82f, s * 0.74f)
                }
                drawPath(p, color, style = st)
            }
            Ico.Skip -> {
                val a = Path().apply { moveTo(s * 0.25f, s * 0.25f); lineTo(s * 0.50f, s * 0.50f); lineTo(s * 0.25f, s * 0.75f) }
                val b = Path().apply { moveTo(s * 0.55f, s * 0.25f); lineTo(s * 0.80f, s * 0.50f); lineTo(s * 0.55f, s * 0.75f) }
                drawPath(a, color, style = st)
                drawPath(b, color, style = st)
            }
        }
    }
}

/** Translucent glass panel that lets the living background shine through. */
@Composable
fun GlassCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x38FFFFFF), Color(0x12FFFFFF))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0xAAFFFFFF), Color(0x14FFFFFF), Color(0x55FFFFFF))), shape)
            .padding(16.dp),
        content = content
    )
}

/** Liquid-glass bubble button: gloss, glint, rim light, gentle floating and a squish on press. */
@Composable
fun BubbleButton(
    text: String,
    icon: Ico,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    phase: Int = 0,
    selected: Boolean = true,
    compact: Boolean = false
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, label = "scale")
    val inf = rememberInfiniteTransition(label = "bob")
    val bob by inf.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(2400 + phase * 370, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bobv"
    )
    val shape = RoundedCornerShape(50)
    val a = if (selected) 1f else 0.5f
    val padH = if (compact) 12.dp else 18.dp
    val padV = if (compact) 7.dp else 12.dp
    val iconBox = if (compact) 22.dp else 30.dp
    Row(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationY = bob * density
            }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.55f * a), tint.copy(alpha = 0.18f * a))))
            .drawBehind {
                drawOval(
                    Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = 0.42f * a), Color.Transparent),
                        startY = size.height * 0.06f,
                        endY = size.height * 0.5f
                    ),
                    topLeft = Offset(size.width * 0.08f, size.height * 0.06f),
                    size = Size(size.width * 0.84f, size.height * 0.44f)
                )
                drawCircle(Color.White.copy(alpha = 0.85f * a), radius = 2.5.dp.toPx(), center = Offset(size.height * 0.5f, size.height * 0.26f))
            }
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.9f * a), Color.White.copy(alpha = 0.12f * a), Color.White.copy(alpha = 0.4f * a))),
                shape
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = padH, vertical = padV),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier
                .size(iconBox)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.45f * a), Color.White.copy(alpha = 0.10f * a))))
                .border(1.dp, Color.White.copy(alpha = 0.5f * a), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            GlassIcon(icon, Modifier.size(iconBox * 0.6f), Color.White.copy(alpha = a))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = Color.White.copy(alpha = a),
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 12.sp else 15.sp
        )
    }
}
