package com.example.space_wallpaper_live

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.graphics.scale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Planet(
    val imageRes: Int,
    val orbitRadius: Float,
    val size: Int,
    val speed: Float,
    val shadowRadius: Float
)

class SolarWallpaperService : WallpaperService() {
    override fun onCreateEngine(): Engine = SolarWallpaperEngine()

    inner class SolarWallpaperEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var visible = false
        private var width = 0f
        private var height = 0f

        private val prefs by lazy {
            getSharedPreferences("wallpaper", MODE_PRIVATE)
        }
        private var time = 0f

        private val earthPeriod = 365.25f

        private val planets = listOf(
            Planet(R.drawable.mercury, 120f, 20, earthPeriod / 87.97f,   13f),
            Planet(R.drawable.venus,   170f, 30, earthPeriod / 224.70f,  18f),
            Planet(R.drawable.earth,   230f, 35, 1.0f,                   17f),
            Planet(R.drawable.mars,    290f, 25, earthPeriod / 686.97f,  15f),
            Planet(R.drawable.jupiter, 380f, 70, earthPeriod / 4332.59f, 38f),
            Planet(R.drawable.saturn,  440f, 120, earthPeriod / 10759.22f, 17f),
            Planet(R.drawable.uranus,  500f, 50, earthPeriod / 30688.50f, 28f),
            Planet(R.drawable.neptune, 560f, 45, earthPeriod / 60182.00f, 21f)
        )

        private lateinit var planetBitmaps: Map<Planet, Bitmap>
        private lateinit var sunBitmap: Bitmap

        private val stars = List(200) {
            Triple(Math.random().toFloat(), Math.random().toFloat(), Math.random().toFloat())
        }

        private val speedScale = 0.05f

        private val drawRunnable = object : Runnable {
            override fun run() {
                draw()
                if (visible) handler.postDelayed(this, 32)
            }
        }

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            time = prefs.getFloat("time", 0f)
            try {
                sunBitmap = loadBitmap(R.drawable.sun, 100, 100)
                planetBitmaps = planets.associateWith { planet ->
                    loadBitmap(planet.imageRes, planet.size, planet.size)
                }
            } catch (e: Exception) {
                Log.e("SolarWallpaper", "Erreur chargement bitmaps", e)
            }
        }

        private fun loadBitmap(res: Int, w: Int, h: Int): Bitmap {
            val bmp = BitmapFactory.decodeResource(resources, res)
            return bmp?.scale(w, h) ?: Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            this.visible = visible
            if (visible) {
                handler.post(drawRunnable)
            } else {
                handler.removeCallbacks(drawRunnable)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
            width = w.toFloat()
            height = h.toFloat()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            visible = false
            handler.removeCallbacks(drawRunnable)
        }

        private fun draw() {
            val holder = surfaceHolder
            if (!holder.surface.isValid || width <= 0f || height <= 0f) return

            val canvas = holder.lockCanvas() ?: return

            try {
                // On évite de sauvegarder dans les prefs à chaque frame pour éviter les lags
                time += 0.02f

                val cx = width / 2f
                val cy = height / 2f

                canvas.drawColor(Color.parseColor("#020B18"))
                drawNebula(canvas, cx, cy)
                drawStars(canvas)
                drawSunGlow(canvas, cx, cy)

                if (::sunBitmap.isInitialized) {
                    canvas.drawBitmap(sunBitmap, cx - 50f, cy - 50f, null)
                }

                val orbitPaint = Paint().apply {
                    color = Color.GRAY
                    alpha = 80
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    isAntiAlias = true
                }

                planets.forEach { planet ->
                    canvas.drawCircle(cx, cy, planet.orbitRadius, orbitPaint)

                    val angle = time * planet.speed * speedScale
                    val x = cx + planet.orbitRadius * cos(angle)
                    val y = cy + planet.orbitRadius * sin(angle)

                    val haloAlpha = (0.05f + 0.05f * ((sin(time * 1.5f + planet.orbitRadius) + 1f) / 2f)) * 255
                    val haloPaint = Paint().apply {
                        color = Color.WHITE
                        alpha = haloAlpha.toInt()
                        isAntiAlias = true
                    }
                    canvas.drawCircle(x, y, planet.shadowRadius + 12f, haloPaint)

                    planetBitmaps[planet]?.let { bmp ->
                        canvas.drawBitmap(bmp, x - planet.size / 2f, y - planet.size / 2f, null)
                    }

                    drawCrescent(canvas, x, y, cx, cy, planet.shadowRadius)
                }

            } finally {
                holder.unlockCanvasAndPost(canvas)
            }
        }

        private fun drawNebula(canvas: Canvas, cx: Float, cy: Float) {
            val paint = Paint().apply { isAntiAlias = true }
            val shader1 = RadialGradient(
                cx * 0.3f,
                cy * 0.4f,
                maxOf(width, height) * 0.6f,
                intArrayOf(Color.parseColor("#CC1A0A3D"), Color.TRANSPARENT),
                null,
                Shader.TileMode.CLAMP
            )
            paint.shader = shader1
            canvas.drawRect(0f, 0f, width, height, paint)
        }

        private fun drawStars(canvas: Canvas) {
            val paint = Paint().apply { isAntiAlias = true; color = Color.WHITE }
            stars.forEach { (xFrac, yFrac, seed) ->
                paint.alpha = ((0.1f + 0.9f * ((sin(time * 1.5f + seed * 100f) + 1f) / 2f)) * 255).toInt()
                canvas.drawCircle(xFrac * width, yFrac * height, if (seed > 0.8f) 3f else 2f, paint)
            }
        }

        private fun drawSunGlow(canvas: Canvas, cx: Float, cy: Float) {
            val paint = Paint().apply { isAntiAlias = true; color = Color.parseColor("#FFD700") }
            listOf(1.4f, 1.7f, 2.0f, 2.4f).forEachIndexed { i, m ->
                paint.alpha = ((0.15f * ((sin(time * 2f + i * 1.2f) + 1f) / 2f)) * 255).toInt()
                canvas.drawCircle(cx, cy, 50f * m, paint)
            }
        }

        private fun drawCrescent(canvas: Canvas, x: Float, y: Float, cx: Float, cy: Float, shadowRadius: Float) {
            val shadowAngle = atan2(y - cy, x - cx)
            val saveCount = canvas.saveLayer(x - shadowRadius*2, y - shadowRadius*2, x + shadowRadius*2, y + shadowRadius*2, null)

            val shadowPaint = Paint().apply { color = Color.BLACK; alpha = 153; isAntiAlias = true }
            canvas.drawCircle(x, y, shadowRadius, shadowPaint)

            val clearPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR); isAntiAlias = true }
            canvas.drawCircle(x - cos(shadowAngle) * (shadowRadius * 0.4f), y - sin(shadowAngle) * (shadowRadius * 0.4f), shadowRadius * 0.9f, clearPaint)

            canvas.restoreToCount(saveCount)
        }

        override fun onDestroy() {
            super.onDestroy()
            handler.removeCallbacks(drawRunnable)
            // Sauvegarde finale de la position
            prefs.edit().putFloat("time", time).apply()
        }
    }
}