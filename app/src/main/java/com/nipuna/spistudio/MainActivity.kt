package com.nipuna.spistudio

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

private val Cyan = Color(0xFF50BEFF)
private val Violet = Color(0xFF9B6BFF)
private val Mint = Color(0xFF3DDC97)
private val Pink = Color(0xFFFF6FB5)

class MainActivity : ComponentActivity() {
    private lateinit var music: MusicPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        music = MusicPlayer(this)
        music.start()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
        )
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Cyan,
                    background = Color.Black,
                    surface = Color.Black,
                    onSurface = Color.White,
                    onBackground = Color.White
                )
            ) {
                Surface(color = Color.Black, modifier = Modifier.fillMaxSize()) { App(music) }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        music.pause()
    }

    override fun onResume() {
        super.onResume()
        music.resume()
    }

    override fun onDestroy() {
        super.onDestroy()
        music.release()
    }
}

@Composable
fun Preview(bmp: Bitmap) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x99000000))
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
fun App(music: MusicPlayer) {
    var tab by remember { mutableIntStateOf(0) }
    var muted by remember { mutableStateOf(false) }
    var started by remember { mutableStateOf(false) }
    val vol by animateFloatAsState(if (!started || muted) 0f else 0.85f, animationSpec = tween(2500), label = "vol")
    LaunchedEffect(Unit) { started = true }
    LaunchedEffect(vol) { music.volume(vol) }
    Box(Modifier.fillMaxSize()) {
        MoodBackground(skipSignal = 0, onName = { })
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text("SPI Studio", fontSize = 30.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Samsung MAET .spi decode / encode  -  by xBooM", fontSize = 11.sp, color = Color(0xFFC4C9D0))
                if (music.available) {
                    Spacer(Modifier.height(8.dp))
                    BubbleButton(
                        text = if (muted) "Music off" else "Music on",
                        icon = if (muted) Ico.Mute else Ico.Sound,
                        tint = Mint,
                        onClick = { muted = !muted },
                        phase = 4,
                        compact = true
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                BubbleButton("Decode", Ico.Image, Cyan, { tab = 0 }, Modifier.weight(1f), phase = 0, selected = tab == 0)
                Spacer(Modifier.width(12.dp))
                BubbleButton("Encode", Ico.Bolt, Pink, { tab = 1 }, Modifier.weight(1f), phase = 1, selected = tab == 1)
            }
            Spacer(Modifier.height(14.dp))
            if (tab == 0) DecodeTab() else EncodeTab()
        }
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
                if (bytes == null) {
                    status = "Cannot read file"
                } else {
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
        GlassCard {
            Text(status, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            BubbleButton("Pick .spi file", Ico.Folder, Cyan, { pick.launch(arrayOf("*/*")) }, Modifier.fillMaxWidth(), phase = 0)
            Spacer(Modifier.height(10.dp))
            BubbleButton(
                "Scan /system/media", Ico.Scan, Violet,
                {
                    val list = File("/system/media").listFiles()
                    files = list?.filter { it.name.endsWith(".spi") }?.sortedBy { it.name } ?: emptyList()
                    status = if (files.isEmpty()) "No .spi files found in /system/media" else files.size.toString() + " files found, tap one"
                },
                Modifier.fillMaxWidth(), phase = 2
            )
        }
        val b = bmp
        if (b != null) {
            Spacer(Modifier.height(14.dp))
            Preview(b)
            Spacer(Modifier.height(10.dp))
            BubbleButton("Save as PNG", Ico.Save, Mint, { save.launch(baseName + ".png") }, Modifier.fillMaxWidth(), phase = 1)
        }
        if (files.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            GlassCard {
                files.forEachIndexed { n, f ->
                    BubbleButton(
                        f.name + "  (" + (f.length() / 1024).toString() + " KB)", Ico.Image, Cyan,
                        {
                            baseName = f.name.removeSuffix(".spi")
                            scope.launch {
                                val bytes = withContext(Dispatchers.IO) { f.readBytes() }
                                decodeBytes(bytes)
                            }
                        },
                        Modifier.fillMaxWidth(), phase = n % 4, compact = true
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        Spacer(Modifier.height(24.dp))
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
                if (bytes == null) {
                    status = "Cannot read file"
                } else {
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
        GlassCard {
            Text(status, color = Color.White, fontSize = 14.sp)
            Spacer(Modifier.height(14.dp))
            BubbleButton("Pick PNG", Ico.Image, Cyan, { pick.launch(arrayOf("image/*")) }, Modifier.fillMaxWidth(), phase = 0)
            val b = bmp
            if (b != null) {
                Spacer(Modifier.height(10.dp))
                BubbleButton(
                    "Encode to .spi", Ico.Bolt, Pink,
                    {
                        scope.launch {
                            status = "Encoding (can take a while for big images)..."
                            val res = withContext(Dispatchers.IO) { Engine.encode(ctx, b) }
                            spi = res.first
                            status = res.second
                        }
                    },
                    Modifier.fillMaxWidth(), phase = 2
                )
            }
            if (spi != null) {
                Spacer(Modifier.height(10.dp))
                BubbleButton("Save .spi", Ico.Save, Mint, { save.launch(baseName + ".spi") }, Modifier.fillMaxWidth(), phase = 1)
            }
        }
        val b2 = bmp
        if (b2 != null) {
            Spacer(Modifier.height(14.dp))
            Preview(b2)
        }
        Spacer(Modifier.height(24.dp))
    }
}
