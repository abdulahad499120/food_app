package com.example.foodapp.ui.screens

import androidx.compose.foundation.Image
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.OrderCompleteViewModel
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCompleteScreen(
    orderId: String,
    onNavigateHome: () -> Unit,
    onNavigateToSupport: (String) -> Unit,
    viewModel: OrderCompleteViewModel = viewModel()
) {
    val submitState by viewModel.submitState.collectAsStateWithLifecycle()
    
    var rating by remember { mutableStateOf(0) }
    var reviewText by remember { mutableStateOf("") }
    
    LaunchedEffect(submitState) {
        if (submitState == true) {
            onNavigateHome()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order Complete", color = TextPrimary) },
                actions = {
                    TextButton(onClick = onNavigateHome) {
                        Text("Skip", color = VAL_BRAND_PRIMARY)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Animated Success Checkmark
            AnimatedCheckmark(modifier = Modifier.size(120.dp))
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "Enjoy your order!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "How was your experience?",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 5-Star Rating Row
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarOutline,
                        contentDescription = "Star $i",
                        tint = if (i <= rating) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier
                            .size(48.dp)
                            .bounceClick { rating = i }
                            .padding(4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = reviewText,
                onValueChange = { reviewText = it },
                label = { Text("Leave a comment (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VAL_BRAND_PRIMARY,
                    unfocusedBorderColor = Color.LightGray
                )
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            PrimaryButton(
                onClick = { viewModel.submitReview(orderId, rating, reviewText) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = rating > 0
            ) {
                Text("Submit Feedback")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedButton(
                onClick = { onNavigateToSupport(orderId) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Need Help? Contact Support", color = VAL_BRAND_PRIMARY)
            }
        }
    }
}

@Composable
fun AnimatedCheckmark(modifier: Modifier = Modifier) {
    val pathMeasure = remember { PathMeasure() }
    val path = remember { Path() }
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animatedProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 600, easing = androidx.compose.animation.core.FastOutSlowInEasing)
        )
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        path.reset()
        // Start from left middle-ish
        path.moveTo(width * 0.2f, height * 0.5f)
        // Draw to bottom center
        path.lineTo(width * 0.45f, height * 0.75f)
        // Draw to top right
        path.lineTo(width * 0.8f, height * 0.25f)

        pathMeasure.setPath(path, false)
        val length = pathMeasure.length
        
        val animatedPath = Path()
        pathMeasure.getSegment(
            startDistance = 0f,
            stopDistance = length * animatedProgress.value,
            destination = animatedPath,
            startWithMoveTo = true
        )

        // Draw a background circle
        drawCircle(
            color = VAL_BRAND_PRIMARY.copy(alpha = 0.1f),
            radius = width / 2
        )

        // Draw the checkmark path
        drawPath(
            path = animatedPath,
            color = VAL_BRAND_PRIMARY,
            style = Stroke(
                width = 8.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
