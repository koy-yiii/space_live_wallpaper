package com.example.space_wallpaper_live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import android.graphics.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.BlendMode
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class Planet(
    val imageRes: Int,
    val orbitRadius: Float,
    val size: Float,
    val speed: Float,
    val shadowRadius: Float = size / 2f
)

@Composable
fun SolarSystemCanvas() {
    val earth_period = 365.25f
    val planets = remember {
        listOf(
            Planet(R.drawable.mercury, 120f, 20f, earth_period / 87.97f),
            Planet(R.drawable.venus, 170f, 30f, earth_period / 224.70f),
            Planet(R.drawable.earth, 230f, 35f, 1.0f),
            Planet(R.drawable.mars, 290f, 25f, earth_period / 686.97f),
            Planet(R.drawable.jupiter, 380f, 70f, earth_period / 4332.59f),
            Planet(R.drawable.saturn, 440f, 120f, earth_period / 10759.22f, shadowRadius = 20f),
            Planet(R.drawable.uranus, 500f, 50f, earth_period / 30688.50f),
            Planet(R.drawable.neptune, 560f, 45f, earth_period / 60182.00f)
        )
    }


    val mercury = ImageBitmap.imageResource(R.drawable.mercury)
    val venus = ImageBitmap.imageResource(R.drawable.venus)
    val earth = ImageBitmap.imageResource(R.drawable.earth)
    val mars = ImageBitmap.imageResource(R.drawable.mars)
    val jupiter = ImageBitmap.imageResource(R.drawable.jupiter)
    val saturn = ImageBitmap.imageResource(R.drawable.saturn)
    val uranus = ImageBitmap.imageResource(R.drawable.uranus)
    val neptune = ImageBitmap.imageResource(R.drawable.neptune)
    val sun = ImageBitmap.imageResource(R.drawable.sun)

    val planetBitmaps = mapOf(
        planets[0] to mercury,
        planets[1] to venus,
        planets[2] to earth,
        planets[3] to mars,
        planets[4] to jupiter,
        planets[5] to saturn,
        planets[6] to uranus,
        planets[7] to neptune
    )

    val stars = remember {
        List(200) {
            Triple(
                (0..10000).random() / 10000f,
                (0..10000).random() / 10000f,
                (0..10000).random() / 10000f
            )
        }
    }

    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("wallpaper", Context.MODE_PRIVATE) }

// Charge la dernière position sauvegardée
    var time by remember { mutableStateOf(prefs.getFloat("time", 0f)) }

    LaunchedEffect(Unit) {
        while (true) {
            time += 0.02f
            // Sauvegarde toutes les 60 frames environ
            if (time % 1f < 0.02f) {
                prefs.edit().putFloat("time", time).apply()
            }
            delay(16)
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f

        // Fond de base noir
        drawRect(color = Color(0xFF020B18))

// Nébuleuse subtile décalée sur le côté
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1A0A3D).copy(alpha = 0.8f),
                    Color(0xFF020B18).copy(alpha = 0.0f)
                ),
                center = Offset(centerX * 0.3f, centerY * 0.4f),
                radius = size.maxDimension * 0.6f
            )
        )

// Deuxième nébuleuse pour donner de la profondeur
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF0A1F3D).copy(alpha = 0.7f),
                    Color(0xFF020B18).copy(alpha = 0.0f)
                ),
                center = Offset(centerX * 1.7f, centerY * 1.5f),
                radius = size.maxDimension * 0.5f
            )
        )
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF1B0A4F).copy(alpha = 0.4f),
                    Color(0xFF020B18).copy(alpha = 0.0f)
                ),
                center = Offset(centerX, centerY),
                radius = size.maxDimension * 0.4f
            )
        )

        // Étoiles
        stars.forEach { (xFrac, yFrac, seed) ->
            val speed = 0.5f + seed * 2f
            val alpha = 0.1f + 0.9f * ((sin(time * 1.5f + seed * 100f) + 1f) / 2f)
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = if (seed > 0.8f) 3f else 2f,
                center = Offset(xFrac * size.width, yFrac * size.height)
            )
        }

        val sunSize = 100
        val glowSizes = listOf(1.4f, 1.7f, 2.0f, 2.4f)
        glowSizes.forEachIndexed { index, multiplier ->
            val glowAlpha = 0.15f * ((sin(time * 2f + index * 1.2f) + 1f) / 2f)
            drawCircle(
                color = Color(0xFFFFD700).copy(alpha = glowAlpha),
                radius = (sunSize / 2f) * multiplier,
                center = Offset(centerX, centerY)
            )
        }

// Halo fixe subtil
        drawCircle(
            color = Color(0xFFFFD700).copy(alpha = 0.08f),
            radius = sunSize.toFloat(),
            center = Offset(centerX, centerY)
        )

        drawImage(
            image = sun,
            dstOffset = IntOffset(
                (centerX - sunSize / 2f).toInt(),
                (centerY - sunSize / 2f).toInt()
            ),
            dstSize = IntSize(sunSize, sunSize)
        )
        val speedScale = 0.05f
        // Orbites
        planets.forEach { planet ->
            drawCircle(
                color = Color.Gray.copy(alpha = 0.3f),
                radius = planet.orbitRadius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 1f)
            )
        }


        // Planètes
        planets.forEach { planet ->
            val angle = time * planet.speed * speedScale
            val x = centerX + planet.orbitRadius * cos(angle)
            val y = centerY + planet.orbitRadius * sin(angle)


            val haloAlpha = 0.1f + 0.1f * ((sin(time * 1.5f + planet.orbitRadius) + 1f) / 2f)
            drawCircle(
                color = Color.White.copy(alpha = haloAlpha),
                radius = planet.size * 0.6f + 12f,
                center = Offset(x, y)
            )

            val bitmap = planetBitmaps[planet]!!
            val size = planet.size.toInt()
            drawImage(
                image = bitmap,
                dstOffset = IntOffset((x - size / 2f).toInt(), (y - size / 2f).toInt()),
                dstSize = IntSize(size, size)
            )
// Ombre croissant
            val shadowAngle = atan2(y - centerY, x - centerX)

            val nativeCanvas = drawContext.canvas.nativeCanvas
            val checkpoint = nativeCanvas.saveLayer(null, null)

            drawCircle(
                color = Color.Black.copy(alpha = 0.6f),
                radius = planet.shadowRadius,
                center = Offset(x, y)
            )

            val crescentOffsetX = cos(shadowAngle) * (planet.shadowRadius * 0.4f)
            val crescentOffsetY = sin(shadowAngle) * (planet.shadowRadius * 0.4f)

            drawCircle(
                color = Color.Transparent,
                radius = planet.shadowRadius * 0.90f,
                center = Offset(x - crescentOffsetX, y - crescentOffsetY),
                blendMode = BlendMode.Clear
            )

            nativeCanvas.restoreToCount(checkpoint)


        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D1A)
@Composable
fun PreviewSolarSystem() {
    SolarSystemCanvas()
}