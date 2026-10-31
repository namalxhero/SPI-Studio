package com.nipuna.spistudio

import android.content.Context
import android.graphics.Bitmap
import java.io.File

/** Talks to the native helper (libspitool.so) which drives the device's own /system/lib64/libmaet.so */
object Engine {
    data class Res(val code: Int, val out: String, val err: String)

    private fun tool(ctx: Context): String = File(ctx.applicationInfo.nativeLibraryDir, "libspitool.so").absolutePath

    private fun exec(cmd: List<String>): Res {
        return try {
            val p = ProcessBuilder(cmd).start()
            val out = p.inputStream.bufferedReader().readText()
            val err = p.errorStream.bufferedReader().readText()
            Res(p.waitFor(), out.trim(), err.trim())
        } catch (e: Exception) {
            Res(-1, "", e.toString())
        }
    }

    /** Runs the helper normally; if the linker refuses libmaet for apps, retries through su. */
    private fun run(ctx: Context, args: List<String>): Res {
        val plain = exec(listOf(tool(ctx)) + args)
        if (plain.code == 0 || !plain.err.contains("dlopen")) return plain
        val line = (listOf(tool(ctx)) + args).joinToString(" ") { "'" + it + "'" }
        return exec(listOf("su", "-c", line))
    }

    fun decode(ctx: Context, spi: ByteArray): Pair<Bitmap?, String> {
        val inF = File(ctx.cacheDir, "in.spi")
        val outF = File(ctx.cacheDir, "out.rgba")
        inF.writeBytes(spi)
        outF.delete()
        val r = run(ctx, listOf("dec", inF.absolutePath, outF.absolutePath))
        if (r.code != 0) return null to ("decode failed (" + r.code + ") " + r.err)
        val dims = r.out.trim().split(Regex("\\s+"))
        if (dims.size < 2) return null to "bad helper output"
        val w = dims[0].toInt()
        val h = dims[1].toInt()
        val raw = outF.readBytes()
        val px = IntArray(w * h)
        for (i in 0 until w * h) {
            val o = i * 4
            px[i] = ((raw[o + 3].toInt() and 255) shl 24) or
                ((raw[o].toInt() and 255) shl 16) or
                ((raw[o + 1].toInt() and 255) shl 8) or
                (raw[o + 2].toInt() and 255)
        }
        val bmp = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
        return bmp to ("Decoded " + w + "x" + h)
    }

    fun encode(ctx: Context, bmp: Bitmap): Pair<ByteArray?, String> {
        val w = bmp.width
        val h = bmp.height
        if (w > 8192 || h > 8192) return null to "image too large (max 8192)"
        val px = IntArray(w * h)
        bmp.getPixels(px, 0, w, 0, 0, w, h)
        val raw = ByteArray(w * h * 4)
        for (i in 0 until w * h) {
            val c = px[i]
            val o = i * 4
            raw[o] = ((c shr 16) and 255).toByte()
            raw[o + 1] = ((c shr 8) and 255).toByte()
            raw[o + 2] = (c and 255).toByte()
            raw[o + 3] = ((c shr 24) and 255).toByte()
        }
        val inF = File(ctx.cacheDir, "in.rgba")
        val outF = File(ctx.cacheDir, "out.spi")
        inF.writeBytes(raw)
        outF.delete()
        val r = run(ctx, listOf("enc", inF.absolutePath, w.toString(), h.toString(), outF.absolutePath))
        if (r.code != 0) return null to ("encode failed (" + r.code + ") " + r.err)
        return outF.readBytes() to r.out
    }
}
