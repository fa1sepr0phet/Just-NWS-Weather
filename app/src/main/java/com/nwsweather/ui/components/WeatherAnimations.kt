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
import kotlin.random.Random

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
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rain_progress"
    )

    val raindrops = remember {
        List(50) {
            Raindrop(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                speed = 0.5f + Random.nextFloat() * 0.5f,
                length = 10f + Random.nextFloat() * 20f
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
                end = Offset(x, y + drop.length),
                strokeWidth = 2f
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
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "snow_progress"
    )

    val snowflakes = remember {
        List(40) {
            Snowflake(
                x = Random.nextFloat(),
                yOffset = Random.nextFloat(),
                speed = 0.2f + Random.nextFloat() * 0.3f,
                radius = 2f + Random.nextFloat() * 4f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        snowflakes.forEach { flake ->
            val y = ((flake.yOffset + progress * flake.speed) % 1f) * size.height
            val x = (flake.x * size.width) + (Math.sin(progress.toDouble() * 2 * Math.PI + flake.yOffset).toFloat() * 20f)
            drawCircle(
                color = Color.White.copy(alpha = 0.7f),
                radius = flake.radius,
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

private data class Raindrop(val x: Float, val yOffset: Float, val speed: Float, val length: Float)
private data class Snowflake(val x: Float, val yOffset: Float, val speed: Float, val radius: Float)
