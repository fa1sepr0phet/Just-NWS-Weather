package com.nwsweather.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.nwsweather.ui.theme.WeatherMood
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

// One loop of each animation. Drop and flake speeds are whole screen passes per loop.
private const val RAIN_LOOP_MS = 4000
private const val SNOW_LOOP_MS = 20000
private const val SNOW_SWAY_CYCLES = 5

@Composable
fun WeatherAtmosphereAnimation(mood: WeatherMood) {
    Box(modifier = Modifier.fillMaxSize()) {
        when (mood) {
            WeatherMood.RAIN, WeatherMood.STORM -> FallingRain()
            WeatherMood.SUNNY -> SunnyAnimation()
            WeatherMood.SNOW -> FallingSnow()
            else -> {}
        }
    }
}

@Composable
fun FallingRain() {
    val infiniteTransition = rememberInfiniteTransition(label = "rain")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(RAIN_LOOP_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_progress"
    )

    val raindrops = remember {
        List(50) {
            Raindrop(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                // Whole numbers only: each drop must finish a whole number of
                // screen passes per loop, or it jumps when the loop restarts.
                speed = Random.nextInt(2, 5).toFloat(),
                length = 4f + Random.nextFloat() * 8f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        raindrops.forEach { drop ->
            val y = ((drop.yOffset + progress * drop.speed) % 1f) * size.height
            val x = drop.x * size.width
            drawLine(
                color = Color.White.copy(alpha = 0.3f),
                start = Offset(x, y),
                end = Offset(x, y + drop.length.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

@Composable
fun FallingSnow() {
    val infiniteTransition = rememberInfiniteTransition(label = "snow")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(SNOW_LOOP_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_progress"
    )

    val snowflakes = remember {
        List(40) {
            Snowflake(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                speed = Random.nextInt(1, 4).toFloat(),
                radius = 1f + Random.nextFloat() * 1.5f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        snowflakes.forEach { flake ->
            val y = ((flake.yOffset + progress * flake.speed) % 1f) * size.height
            // The sway also completes a whole number of cycles per loop.
            val sway = sin(progress * 2.0 * PI * SNOW_SWAY_CYCLES + flake.yOffset * 2.0 * PI).toFloat()
            val x = (flake.x * size.width) + sway * 8.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = flake.radius.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}

@Composable
fun SunnyAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "sun")
    
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sun_rotation"
    )

    // Sun entry animation
    val entryProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        entryProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val sunCenter = Offset(
            size.width - 88.dp.toPx(),
            180.dp.toPx() + (1f - entryProgress.value) * 24.dp.toPx()
        )
        val radius = 60.dp.toPx()

        rotate(rotation, sunCenter) {
            // Draw Rays
            for (i in 0 until 12) {
                rotate(i * 30f, sunCenter) {
                    drawLine(
                        color = Color(0xFFFFE082).copy(alpha = 0.6f),
                        start = Offset(sunCenter.x, sunCenter.y - radius - 10f),
                        end = Offset(sunCenter.x, sunCenter.y - radius - 40f),
                        strokeWidth = 8f
                    )
                }
            }
        }

        drawCircle(
            color = Color(0xFFFFD54F),
            radius = radius,
            center = sunCenter
        )
    }
}

/** x and yOffset are fractions of the screen; length and radius are in dp. */
private data class Raindrop(val x: Float, val yOffset: Float, val speed: Float, val length: Float)
private data class Snowflake(val x: Float, val yOffset: Float, val speed: Float, val radius: Float)
