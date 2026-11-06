package com.example.domain.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

object GenerativeArtEngine {

    enum class ArtStyle(val label: String, val promptDescriptor: String) {
        CYBERPUNK("Cyberpunk", "Neon skyline, cyan-magenta glow, holographic grids"),
        COSMIC("Cosmic Sci-Fi", "Nebula swirls, glowing celestial bodies, starfields"),
        ANIME_SCENIC("Anime Twilight", "Pastel evening sky, floating cherry petals, mountain silhouettes"),
        FANTASY_REALM("Mystic Fantasy", "Enchanted bioluminescent flora, ancient ruins, twilight aura"),
        RETRO_SYNTH("Retro Synthwave", "80s grid horizon, giant segmented sun, wireframe mountains"),
        MINIMALIST("Minimalist 3D", "Floating geometric solids, clean lighting, atmospheric depth"),
        VAPORWAVE("Vaporwave", "Pastel aesthetic, marble pillars, nostalgic sunset horizon")
    }

    suspend fun generateArtwork(
        context: Context,
        prompt: String,
        style: ArtStyle = ArtStyle.CYBERPUNK,
        width: Int = 800,
        height: Int = 800,
        seed: Long = System.currentTimeMillis()
    ): String = withContext(Dispatchers.IO) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val random = Random(seed + prompt.hashCode())

        // Choose palette and painter based on style
        when (style) {
            ArtStyle.CYBERPUNK -> drawCyberpunk(canvas, width, height, random, prompt)
            ArtStyle.COSMIC -> drawCosmic(canvas, width, height, random, prompt)
            ArtStyle.ANIME_SCENIC -> drawAnimeScenic(canvas, width, height, random, prompt)
            ArtStyle.FANTASY_REALM -> drawFantasyRealm(canvas, width, height, random, prompt)
            ArtStyle.RETRO_SYNTH -> drawRetroSynth(canvas, width, height, random, prompt)
            ArtStyle.MINIMALIST -> drawMinimalist(canvas, width, height, random, prompt)
            ArtStyle.VAPORWAVE -> drawVaporwave(canvas, width, height, random, prompt)
        }

        // Save to file
        val imagesDir = File(context.filesDir, "anima_art").apply { mkdirs() }
        val file = File(imagesDir, "art_${System.currentTimeMillis()}_${seed % 10000}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()
        file.absolutePath
    }

    private fun drawCyberpunk(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Dark city night gradient
        val bgShader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                AndroidColor.rgb(10, 8, 28),
                AndroidColor.rgb(20, 10, 45),
                AndroidColor.rgb(5, 12, 30)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Neon giant moon/hologram
        val moonX = w * (0.3f + rng.nextFloat() * 0.4f)
        val moonY = h * 0.35f
        val moonRad = w * 0.22f
        val moonShader = RadialGradient(
            moonX, moonY, moonRad,
            intArrayOf(
                AndroidColor.argb(240, 255, 0, 128),
                AndroidColor.argb(180, 0, 229, 255),
                AndroidColor.TRANSPARENT
            ),
            floatArrayOf(0f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = moonShader
        canvas.drawCircle(moonX, moonY, moonRad, paint)
        paint.shader = null

        // Distant skyscrapers silhouette
        paint.color = AndroidColor.rgb(12, 16, 32)
        paint.style = Paint.Style.FILL
        var curX = 0f
        while (curX < w) {
            val bWidth = (w * (0.04f + rng.nextFloat() * 0.08f))
            val bHeight = (h * (0.25f + rng.nextFloat() * 0.35f))
            canvas.drawRect(curX, h - bHeight, curX + bWidth, h.toFloat(), paint)

            // Neon roof antenna or beacon
            if (rng.nextBoolean()) {
                val beaconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (rng.nextBoolean()) AndroidColor.CYAN else AndroidColor.rgb(255, 64, 129)
                    strokeWidth = 3f
                }
                canvas.drawLine(curX + bWidth * 0.5f, h - bHeight, curX + bWidth * 0.5f, h - bHeight - 30f, beaconPaint)
            }
            curX += bWidth - 4f
        }

        // Foreground detailed skyscrapers with lit windows
        paint.color = AndroidColor.rgb(6, 8, 18)
        curX = -20f
        while (curX < w + 20) {
            val bWidth = (w * (0.07f + rng.nextFloat() * 0.10f))
            val bHeight = (h * (0.15f + rng.nextFloat() * 0.30f))
            val top = h - bHeight
            canvas.drawRect(curX, top, curX + bWidth, h.toFloat(), paint)

            // Windows
            val winCols = 3 + rng.nextInt(4)
            val winRows = 6 + rng.nextInt(10)
            val colStep = bWidth / (winCols + 1)
            val rowStep = bHeight / (winRows + 1)

            val winPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            for (r in 1..winRows) {
                for (c in 1..winCols) {
                    if (rng.nextFloat() > 0.4f) {
                        winPaint.color = when (rng.nextInt(4)) {
                            0 -> AndroidColor.argb(220, 0, 229, 255)
                            1 -> AndroidColor.argb(220, 255, 0, 128)
                            2 -> AndroidColor.argb(200, 255, 230, 100)
                            else -> AndroidColor.argb(160, 120, 255, 200)
                        }
                        val wx = curX + c * colStep
                        val wy = top + r * rowStep
                        canvas.drawRect(wx - 2f, wy - 3f, wx + 3f, wy + 3f, winPaint)
                    }
                }
            }
            curX += bWidth + 5f
        }

        // Perspective glowing wet ground grid
        val gridHorizon = h * 0.82f
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(120, 0, 229, 255)
            strokeWidth = 2f
        }
        for (i in 0..14) {
            val progress = i / 14f
            val y = gridHorizon + (h - gridHorizon) * (progress * progress)
            linePaint.alpha = (50 + progress * 160).toInt()
            canvas.drawLine(0f, y, w.toFloat(), y, linePaint)
        }
        // Vanishing perspective rays
        for (i in -10..22) {
            val groundX = w * 0.5f + i * (w * 0.08f)
            canvas.drawLine(w * 0.5f, gridHorizon, groundX, h.toFloat(), linePaint)
        }

        // Floating holographic digital particles / rain streaks
        val streakPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(180, 0, 229, 255)
            strokeWidth = 2f
        }
        for (i in 0..60) {
            val rx = rng.nextFloat() * w
            val ry = rng.nextFloat() * h
            val len = 10f + rng.nextFloat() * 35f
            streakPaint.color = if (rng.nextBoolean()) AndroidColor.argb(140, 0, 229, 255) else AndroidColor.argb(140, 255, 0, 128)
            canvas.drawLine(rx, ry, rx - 3f, ry + len, streakPaint)
        }
    }

    private fun drawCosmic(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Deep cosmos background
        val bgShader = RadialGradient(
            w * 0.5f, h * 0.5f, w * 0.8f,
            intArrayOf(
                AndroidColor.rgb(25, 12, 50),
                AndroidColor.rgb(10, 5, 25),
                AndroidColor.rgb(3, 2, 10)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Nebula clouds with soft multi-layer radials
        for (i in 0..3) {
            val nx = w * (0.2f + rng.nextFloat() * 0.6f)
            val ny = h * (0.2f + rng.nextFloat() * 0.6f)
            val nRad = w * (0.3f + rng.nextFloat() * 0.35f)
            val col = when (i % 3) {
                0 -> AndroidColor.argb(110, 180, 50, 255)
                1 -> AndroidColor.argb(90, 0, 200, 255)
                else -> AndroidColor.argb(85, 255, 60, 150)
            }
            val nebShader = RadialGradient(
                nx, ny, nRad,
                intArrayOf(col, AndroidColor.TRANSPARENT),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
            paint.shader = nebShader
            canvas.drawCircle(nx, ny, nRad, paint)
        }
        paint.shader = null

        // Thousand twinkling stars
        val starPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0..300) {
            val sx = rng.nextFloat() * w
            val sy = rng.nextFloat() * h
            val sRad = 0.5f + rng.nextFloat() * 2.2f
            val bright = 120 + rng.nextInt(135)
            starPaint.color = AndroidColor.argb(bright, 240, 245, 255)
            canvas.drawCircle(sx, sy, sRad, starPaint)
        }

        // Large Majestic Ringed Exoplanet
        val px = w * 0.35f
        val py = h * 0.42f
        val pRad = w * 0.22f
        val planetShader = RadialGradient(
            px - pRad * 0.3f, py - pRad * 0.3f, pRad * 1.3f,
            intArrayOf(
                AndroidColor.rgb(255, 140, 80),
                AndroidColor.rgb(160, 40, 110),
                AndroidColor.rgb(20, 10, 40)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = planetShader
        canvas.drawCircle(px, py, pRad, paint)
        paint.shader = null

        // Rings around planet
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 14f
            color = AndroidColor.argb(170, 255, 200, 150)
        }
        canvas.save()
        canvas.translate(px, py)
        canvas.rotate(-28f)
        canvas.drawOval(-pRad * 2.1f, -pRad * 0.38f, pRad * 2.1f, pRad * 0.38f, ringPaint)
        ringPaint.strokeWidth = 6f
        ringPaint.color = AndroidColor.argb(120, 120, 220, 255)
        canvas.drawOval(-pRad * 2.3f, -pRad * 0.42f, pRad * 2.3f, pRad * 0.42f, ringPaint)
        canvas.restore()

        // Smaller sister moon
        val smX = w * 0.78f
        val smY = h * 0.25f
        val smRad = w * 0.05f
        val smPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(180, 220, 255)
        }
        canvas.drawCircle(smX, smY, smRad, smPaint)
    }

    private fun drawAnimeScenic(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Sunset pastel sky
        val skyShader = LinearGradient(
            0f, 0f, 0f, h * 0.7f,
            intArrayOf(
                AndroidColor.rgb(45, 30, 85),
                AndroidColor.rgb(130, 60, 125),
                AndroidColor.rgb(240, 130, 110),
                AndroidColor.rgb(255, 200, 130)
            ),
            floatArrayOf(0f, 0.35f, 0.7f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = skyShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Soft setting golden sun
        val sunX = w * 0.65f
        val sunY = h * 0.45f
        val sunRad = w * 0.16f
        val sunShader = RadialGradient(
            sunX, sunY, sunRad * 1.5f,
            intArrayOf(
                AndroidColor.argb(250, 255, 255, 230),
                AndroidColor.argb(180, 255, 180, 80),
                AndroidColor.TRANSPARENT
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = sunShader
        canvas.drawCircle(sunX, sunY, sunRad * 1.5f, paint)
        paint.shader = null

        // Distant majestic mountains
        paint.style = Paint.Style.FILL
        drawMountainRange(canvas, w, h, baseHeight = h * 0.52f, peakVar = h * 0.14f, AndroidColor.argb(200, 95, 55, 110), rng)
        drawMountainRange(canvas, w, h, baseHeight = h * 0.65f, peakVar = h * 0.12f, AndroidColor.argb(240, 48, 30, 70), rng)

        // Calming reflective lake
        val lakeTop = h * 0.72f
        val lakeShader = LinearGradient(
            0f, lakeTop, 0f, h.toFloat(),
            intArrayOf(
                AndroidColor.rgb(65, 45, 95),
                AndroidColor.rgb(25, 18, 45)
            ),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = lakeShader
        canvas.drawRect(0f, lakeTop, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Sun reflection in lake
        val reflPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(130, 255, 200, 140)
            strokeWidth = 3f
        }
        for (i in 0..20) {
            val ry = lakeTop + i * ((h - lakeTop) / 20f)
            val spread = 20f + i * 8f
            canvas.drawLine(sunX - spread, ry, sunX + spread, ry, reflPaint)
        }

        // Torii gate or shrine silhouette on foreground cliff
        val cliffPath = Path().apply {
            moveTo(0f, h * 0.82f)
            lineTo(w * 0.35f, h * 0.78f)
            lineTo(w * 0.42f, h.toFloat())
            lineTo(0f, h.toFloat())
            close()
        }
        paint.color = AndroidColor.rgb(18, 12, 28)
        canvas.drawPath(cliffPath, paint)

        // Draw classic Torii gate on cliff
        val gateX = w * 0.22f
        val gateY = h * 0.73f
        val gateW = w * 0.10f
        val gateH = h * 0.08f
        val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(180, 40, 40)
            strokeWidth = 6f
            style = Paint.Style.STROKE
        }
        canvas.drawLine(gateX - gateW * 0.5f, gateY + gateH, gateX - gateW * 0.5f, gateY, gatePaint)
        canvas.drawLine(gateX + gateW * 0.5f, gateY + gateH, gateX + gateW * 0.5f, gateY, gatePaint)
        canvas.drawLine(gateX - gateW * 0.65f, gateY + 5f, gateX + gateW * 0.65f, gateY + 5f, gatePaint)
        canvas.drawLine(gateX - gateW * 0.75f, gateY - 6f, gateX + gateW * 0.75f, gateY - 6f, gatePaint)

        // Floating Sakura petals in the breeze
        val petalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(210, 255, 180, 200)
            style = Paint.Style.FILL
        }
        for (i in 0..45) {
            val px = rng.nextFloat() * w
            val py = rng.nextFloat() * h
            canvas.drawOval(px, py, px + 7f, py + 4f, petalPaint)
        }
    }

    private fun drawMountainRange(canvas: Canvas, w: Int, h: Int, baseHeight: Float, peakVar: Float, color: Int, rng: Random) {
        val path = Path().apply {
            moveTo(0f, h.toFloat())
            lineTo(0f, baseHeight)
            var curX = 0f
            while (curX < w) {
                val step = w * (0.12f + rng.nextFloat() * 0.15f)
                val peakY = baseHeight - (rng.nextFloat() * peakVar)
                val nextX = (curX + step).coerceAtMost(w.toFloat())
                lineTo((curX + nextX) * 0.5f, peakY)
                lineTo(nextX, baseHeight + rng.nextFloat() * 15f)
                curX = nextX
            }
            lineTo(w.toFloat(), h.toFloat())
            close()
        }
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            style = Paint.Style.FILL
        }
        canvas.drawPath(path, paint)
    }

    private fun drawFantasyRealm(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Mystic twilight teal/indigo background
        val bgShader = LinearGradient(
            0f, 0f, 0f, h.toFloat(),
            intArrayOf(
                AndroidColor.rgb(8, 20, 35),
                AndroidColor.rgb(15, 45, 55),
                AndroidColor.rgb(5, 25, 25)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        paint.shader = null

        // Enchanted magical crystal or ancient tree glow
        val glowX = w * 0.5f
        val glowY = h * 0.6f
        val radGlow = w * 0.35f
        val radShader = RadialGradient(
            glowX, glowY, radGlow,
            intArrayOf(
                AndroidColor.argb(220, 100, 255, 218),
                AndroidColor.argb(120, 30, 180, 200),
                AndroidColor.TRANSPARENT
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = radShader
        canvas.drawCircle(glowX, glowY, radGlow, paint)
        paint.shader = null

        // Luminescent floating spore fireflies
        val sporePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0..120) {
            val sx = rng.nextFloat() * w
            val sy = rng.nextFloat() * h
            val size = 2f + rng.nextFloat() * 5f
            sporePaint.color = if (rng.nextBoolean()) {
                AndroidColor.argb(220, 130, 255, 220)
            } else {
                AndroidColor.argb(200, 255, 215, 120)
            }
            canvas.drawCircle(sx, sy, size, sporePaint)
        }

        // Giant ancient enchanted tree silhouette
        val trunkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(6, 15, 20)
            style = Paint.Style.FILL
        }
        val trunkPath = Path().apply {
            moveTo(w * 0.42f, h.toFloat())
            quadTo(w * 0.46f, h * 0.7f, w * 0.44f, h * 0.4f)
            lineTo(w * 0.56f, h * 0.4f)
            quadTo(w * 0.54f, h * 0.7f, w * 0.58f, h.toFloat())
            close()
        }
        canvas.drawPath(trunkPath, trunkPaint)

        // Glowing foliage puffs
        val leafPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0..15) {
            val lx = w * (0.3f + rng.nextFloat() * 0.4f)
            val ly = h * (0.2f + rng.nextFloat() * 0.3f)
            val lRad = w * (0.1f + rng.nextFloat() * 0.12f)
            leafPaint.color = AndroidColor.argb(190, 15, 60 + rng.nextInt(60), 65 + rng.nextInt(40))
            canvas.drawCircle(lx, ly, lRad, leafPaint)
        }
    }

    private fun drawRetroSynth(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // 80s gradient
        val bgShader = LinearGradient(
            0f, 0f, 0f, h * 0.65f,
            intArrayOf(
                AndroidColor.rgb(18, 5, 45),
                AndroidColor.rgb(80, 15, 75),
                AndroidColor.rgb(180, 30, 95)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = bgShader
        canvas.drawRect(0f, 0f, w.toFloat(), h * 0.65f, paint)
        paint.shader = null

        // Big segmented retro sun
        val sunX = w * 0.5f
        val sunY = h * 0.45f
        val sunRad = w * 0.28f
        val sunShader = LinearGradient(
            0f, sunY - sunRad, 0f, sunY + sunRad,
            intArrayOf(
                AndroidColor.rgb(255, 235, 60),
                AndroidColor.rgb(255, 80, 80),
                AndroidColor.rgb(255, 20, 140)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = sunShader
        canvas.drawCircle(sunX, sunY, sunRad, paint)
        paint.shader = null

        // Horizontal sun cuts (synthwave blinds)
        val cutPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(80, 15, 75)
        }
        for (i in 1..8) {
            val cutY = sunY + (i / 9f) * sunRad
            val cutH = 2f + i * 2.2f
            canvas.drawRect(sunX - sunRad, cutY, sunX + sunRad, cutY + cutH, cutPaint)
        }

        // Glowing Wireframe Ground Grid
        val groundTop = h * 0.65f
        val groundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.rgb(10, 2, 22)
            style = Paint.Style.FILL
        }
        canvas.drawRect(0f, groundTop, w.toFloat(), h.toFloat(), groundPaint)

        val neonPink = AndroidColor.rgb(255, 0, 128)
        val neonCyan = AndroidColor.rgb(0, 240, 255)
        val gridLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = neonPink
            strokeWidth = 2.5f
        }

        // Horizontal receding lines
        for (i in 0..12) {
            val ratio = i / 12f
            val y = groundTop + (h - groundTop) * (ratio * ratio)
            canvas.drawLine(0f, y, w.toFloat(), y, gridLinePaint)
        }
        // Vertical perspective lines
        gridLinePaint.color = neonCyan
        for (i in -8..18) {
            val gx = w * 0.5f + i * (w * 0.09f)
            canvas.drawLine(w * 0.5f, groundTop, gx, h.toFloat(), gridLinePaint)
        }
    }

    private fun drawMinimalist(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        // Clean high-key architectural warm background
        paint.color = AndroidColor.rgb(245, 242, 238)
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)

        // Floating geometric shapes with soft shadows
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = AndroidColor.argb(30, 20, 20, 30)
        }
        val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Big sphere
        val cx = w * 0.45f
        val cy = h * 0.45f
        val rad = w * 0.25f
        canvas.drawOval(cx - rad * 0.8f, cy + rad * 0.9f, cx + rad * 1.2f, cy + rad * 1.3f, shadowPaint)

        val sphereShader = RadialGradient(
            cx - rad * 0.3f, cy - rad * 0.3f, rad * 1.2f,
            intArrayOf(
                AndroidColor.rgb(255, 120, 90),
                AndroidColor.rgb(230, 60, 80),
                AndroidColor.rgb(80, 20, 40)
            ),
            floatArrayOf(0f, 0.6f, 1f),
            Shader.TileMode.CLAMP
        )
        shapePaint.shader = sphereShader
        canvas.drawCircle(cx, cy, rad, shapePaint)
        shapePaint.shader = null

        // Floating arch / ring
        val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 24f
            color = AndroidColor.rgb(40, 60, 95)
        }
        canvas.drawArc(
            w * 0.2f, h * 0.2f, w * 0.8f, h * 0.8f,
            200f, 160f, false, ringPaint
        )
    }

    private fun drawVaporwave(canvas: Canvas, w: Int, h: Int, rng: Random, prompt: String) {
        drawRetroSynth(canvas, w, h, rng, prompt)
    }
}
