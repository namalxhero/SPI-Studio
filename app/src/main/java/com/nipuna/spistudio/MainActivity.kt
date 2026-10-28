package com.nipuna.spistudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import android.media.MediaPlayer
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

private val Accent = Color(0xFF50BEFF)

class MainActivity : ComponentActivity() {
    private var intro: MediaPlayer? = null

    // Plays app/src/main/assets/intro.mp3 once at launch. No UI, no player. Silent if the file is missing.
    private fun playIntro() {
        try {
            val afd = assets.openFd("intro.mp3")
            val mp = MediaPlayer()
            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            mp.setOnCompletionListener { it.release(); if (intro === it) intro = null }
            mp.prepare()
            mp.start()
            intro = mp
        } catch (e: Exception) {
            intro = null
        }
    }

    override fun onDestroy() {
        intro?.release()
        intro = null
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) playIntro()
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Accent,
                    background = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                Surface(color = Color.Transparent, modifier = Modifier.fillMaxSize()) {
                    Box(Modifier.fillMaxSize()) { NatureBackground(); App() }
                }
            }
        }
    }
}

private class Mood(val top: Color, val bottom: Color, val leaf: Color)

// every MOOD_SECONDS the sky slowly melts into the next mood (dawn -> forest -> ocean -> dusk -> night)
private const val MOOD_SECONDS = 60f
private val Moods = listOf(
    Mood(Color(0xFF1B2A49), Color(0xFF8A4F6D), Color(0xFFFFC9A8)),
    Mood(Color(0xFF0F3D3A), Color(0xFF2E7D5B), Color(0xFFB6F2B0)),
    Mood(Color(0xFF0B2D4F), Color(0xFF2A7FA8), Color(0xFFA8E6E0)),
    Mood(Color(0xFF2A1040), Color(0xFFB04A6A), Color(0xFFFFB38A)),
    Mood(Color(0xFF050B1E), Color(0xFF1B2A5A), Color(0xFF8FB8FF))
)

private fun smooth(x: Float) = x * x * (3f - 2f * x)

private class Leaf(val x: Float, val speed: Float, val size: Float, val phase: Float, val sway: Float)

private val Leaves = List(16) { i ->
    val r = { k: Int -> ((i * 7919 + k * 104729) % 1000) / 1000f }
    Leaf(x = r(1), speed = 0.012f + r(2) * 0.018f, size = 10f + r(3) * 16f, phase = r(4) * 6.28f, sway = 0.02f + r(5) * 0.05f)
}

@Composable
fun NatureBackground() {
    var t by remember { mutableStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withFrameNanos { it }
        while (true) withFrameNanos { t = (it - start) / 1_000_000_000f }
    }
    Canvas(Modifier.fillMaxSize()) {
        val pos = t / MOOD_SECONDS
        val i = pos.toInt()
        val a = Moods[i % Moods.size]
        val b = Moods[(i + 1) % Moods.size]
        val f = smooth(pos - i)
        val top = lerp(a.top, b.top, f)
        val bottom = lerp(a.bottom, b.bottom, f)
        val leafColor = lerp(a.leaf, b.leaf, f)
        drawRect(Brush.verticalGradient(listOf(top, bottom)))
        // soft drifting light blob
        val bx = size.width * (0.5f + 0.3f * sin(t * 0.07f))
        val by = size.height * (0.35f + 0.1f * sin(t * 0.05f + 1f))
        drawCircle(
            Brush.radialGradient(listOf(leafColor.copy(alpha = 0.22f), Color.Transparent), center = Offset(bx, by), radius = size.width * 0.7f),
            radius = size.width * 0.7f, center = Offset(bx, by)
        )
        // leaves swaying in the wind while they float down
        Leaves.forEach { l ->
            val y = (((t * l.speed + l.phase) % 1f) * 1.2f - 0.1f) * size.height
            val x = (l.x + sin(t * 0.5f + l.phase) * l.sway) * size.width
            val ang = sin(t * 0.8f + l.phase) * 35f
            val s = l.size * density
            val path = Path().apply {
                moveTo(0f, -s)
                cubicTo(s * 0.9f, -s * 0.5f, s * 0.7f, s * 0.6f, 0f, s)
                cubicTo(-s * 0.7f, s * 0.6f, -s * 0.9f, -s * 0.5f, 0f, -s)
                close()
            }
            translateTo(x, y) { rotate(ang, Offset.Zero) { drawPath(path, leafColor.copy(alpha = 0.28f)) } }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.translateTo(
    x: Float, y: Float, block: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit
) = androidx.compose.ui.graphics.drawscope.withTransform({ translate(x, y) }, block)

// liquid-glass bubble button: translucent body, specular highlight, glowing rim, squishy press
@Composable
fun GlassButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Color.White,
    compact: Boolean = false
) {
    val shape = RoundedCornerShape(50)
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.94f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bubble"
    )
    Box(
        modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(10.dp, shape, ambientColor = tint, spotColor = tint)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(tint.copy(alpha = if (pressed) 0.55f else 0.38f), tint.copy(alpha = 0.12f))
                )
            )
            .border(
                1.2.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.85f), Color.White.copy(alpha = 0.08f), tint.copy(alpha = 0.45f))),
                shape
            )
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.40f), Color.Transparent), endY = size.height * 0.55f),
                    topLeft = Offset(size.width * 0.07f, 2.dp.toPx()),
                    size = Size(size.width * 0.86f, size.height * 0.48f),
                    cornerRadius = CornerRadius(size.height / 2)
                )
            }
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = if (compact) 9.dp else 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(if (compact) 16.dp else 20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, color = Color.White, fontSize = if (compact) 13.sp else 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun Glass(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0x2EFFFFFF), Color(0x0FFFFFFF))))
            .border(1.dp, Brush.verticalGradient(listOf(Color(0x99FFFFFF), Color(0x1AFFFFFF))), shape)
            .padding(16.dp),
        content = content
    )
}

@Composable
fun Preview(bmp: Bitmap) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1A1A20))
            .padding(8.dp)
    ) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.Fit
        )
    }
}

@Composable
fun App() {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text("SPI Studio", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Samsung MAET .spi  decode / encode  -  by xBooM", fontSize = 12.sp, color = Color(0xFF9AA0A6))
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GlassButton("Decode", Icons.Rounded.FileDownload, { tab = 0 }, Modifier.weight(1f), if (tab == 0) Accent else Color.White)
            GlassButton("Encode", Icons.Rounded.FileUpload, { tab = 1 }, Modifier.weight(1f), if (tab == 1) Color(0xFF8B5CF6) else Color.White)
        }
        Spacer(Modifier.height(12.dp))
        if (tab == 0) DecodeTab() else EncodeTab()
    }
}

private suspend fun readUri(ctx: Context, uri: Uri): ByteArray? = withContext(Dispatchers.IO) {
    ctx.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}

@Composable
fun DecodeTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Pick a .spi file, or scan /system/media") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var baseName by remember { mutableStateOf("image") }
    var files by remember { mutableStateOf(listOf<File>()) }

    fun decodeBytes(bytes: ByteArray) {
        scope.launch {
            status = "Decoding..."
            val res = withContext(Dispatchers.IO) { Engine.decode(ctx, bytes) }
            bmp = res.first
            status = res.second
        }
    }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = readUri(ctx, uri)
                if (bytes == null) status = "Cannot read file" else {
                    baseName = "picked"
                    decodeBytes(bytes)
                }
            }
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri: Uri? ->
        val b = bmp
        if (uri != null && b != null) {
            scope.launch {
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)?.use { b.compress(Bitmap.CompressFormat.PNG, 100, it) }
                }
                status = "Saved PNG"
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Glass {
            Text(status, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            GlassButton("Pick .spi file", Icons.Rounded.FolderOpen, { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), Accent)
            Spacer(Modifier.height(8.dp))
            GlassButton("Scan /system/media", Icons.Rounded.Search, {
                val list = File("/system/media").listFiles()
                files = list?.filter { it.name.endsWith(".spi") }?.sortedBy { it.name } ?: emptyList()
                status = if (files.isEmpty()) "No .spi files found in /system/media" else files.size.toString() + " files found, tap one"
            }, Modifier.fillMaxWidth())
        }
        val b = bmp
        if (b != null) {
            Spacer(Modifier.height(12.dp))
            Preview(b)
            Spacer(Modifier.height(8.dp))
            GlassButton("Save as PNG", Icons.Rounded.Save, { save.launch(baseName + ".png") }, Modifier.fillMaxWidth(), Color(0xFF34D399))
        }
        if (files.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            Glass {
                files.forEach { f ->
                    GlassButton(f.name + "  (" + (f.length() / 1024).toString() + " KB)", Icons.Rounded.InsertDriveFile, {
                        baseName = f.name.removeSuffix(".spi")
                        scope.launch {
                            val bytes = withContext(Dispatchers.IO) { f.readBytes() }
                            decodeBytes(bytes)
                        }
                    }, Modifier.fillMaxWidth(), compact = true)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

@Composable
fun EncodeTab() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("Pick a PNG image to encode as .spi (lossless)") }
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var spi by remember { mutableStateOf<ByteArray?>(null) }
    var baseName by remember { mutableStateOf("image") }

    val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bytes = readUri(ctx, uri)
                if (bytes == null) status = "Cannot read file" else {
                    val b = withContext(Dispatchers.IO) { BitmapFactory.decodeByteArray(bytes, 0, bytes.size) }
                    bmp = b
                    spi = null
                    baseName = "encoded"
                    status = if (b == null) "Not a valid image" else "Loaded " + b.width.toString() + "x" + b.height.toString()
                }
            }
        }
    }
    val save = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri: Uri? ->
        val data = spi
        if (uri != null && data != null) {
            scope.launch {
                withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(data) } }
                status = "Saved .spi"
            }
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Glass {
            Text(status, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))
            GlassButton("Pick PNG", Icons.Rounded.Image, { pick.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth(), Accent)
            val b = bmp
            if (b != null) {
                Spacer(Modifier.height(8.dp))
                GlassButton("Encode to .spi", Icons.Rounded.Bolt, {
                    scope.launch {
                        status = "Encoding (can take a while for big images)..."
                        val res = withContext(Dispatchers.IO) { Engine.encode(ctx, b) }
                        spi = res.first
                        status = res.second
                    }
                }, Modifier.fillMaxWidth(), Color(0xFF8B5CF6))
            }
            if (spi != null) {
                Spacer(Modifier.height(8.dp))
                GlassButton("Save .spi", Icons.Rounded.Save, { save.launch(baseName + ".spi") }, Modifier.fillMaxWidth(), Color(0xFF34D399))
            }
        }
        val b2 = bmp
        if (b2 != null) {
            Spacer(Modifier.height(12.dp))
            Preview(b2)
        }
    }
}
