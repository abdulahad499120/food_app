package com.example.foodapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CheckoutSuccessEffect(
    modifier: Modifier = Modifier,
    onAnimationFinished: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var burstTriggered by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        delay(300)
        burstTriggered = true
        delay(1500)
        isVisible = false
        delay(300)
        onAnimationFinished()
    }

    if (isVisible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f)),
            contentAlignment = Alignment.Center
        ) {
            // Main Star
            AnimatedVisibility(
                visible = burstTriggered,
                enter = scaleIn(animationSpec = tween(500, easing = FastOutSlowInEasing)) + fadeIn(tween(500)),
                exit = scaleOut(animationSpec = tween(300)) + fadeOut(tween(300))
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = com.ahad.foodapp.R.drawable.order_success_illustration),
                    contentDescription = "Success",
                    modifier = Modifier.size(240.dp)
                )
            }
            
            // Particles
            if (burstTriggered) {
                ParticleBurst()
            }
        }
    }
}

@Composable
fun ParticleBurst() {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "ParticleProgress"
    )
    
    val particles = remember {
        List(20) {
            Particle(
                angle = Random.nextFloat() * 2 * Math.PI.toFloat(),
                speed = Random.nextFloat() * 1000f + 200f,
                size = Random.nextFloat() * 10f + 10f,
                color = listOf(VAL_BRAND_PRIMARY, Color(0xFFD4AF37), Color.White).random()
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2)
        particles.forEach { particle ->
            val distance = particle.speed * progress
            val x = center.x + cos(particle.angle) * distance
            val y = center.y + sin(particle.angle) * distance
            val alpha = (1f - progress).coerceIn(0f, 1f)
            
            drawCircle(
                color = particle.color.copy(alpha = alpha),
                radius = particle.size,
                center = Offset(x, y)
            )
        }
    }
}

data class Particle(
    val angle: Float,
    val speed: Float,
    val size: Float,
    val color: Color
)
