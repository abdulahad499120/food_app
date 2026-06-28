package com.example.foodapp.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToConfirm(
    text: String = "Slide to Confirm",
    onConfirm: () -> Unit
) {
    val trackColor = Color(0xFFFFD54F) // Sunshine Yellow
    val handleColor = Color(0xFFFFFFFF) // Cream White
    val textColor = Color(0xFF3E2723) // Deep Cocoa
    val iconColor = Color(0xFFB80049) // Primary

    var trackWidth by remember { mutableIntStateOf(0) }
    var handleOffset by remember { mutableFloatStateOf(0f) }
    var isConfirmed by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    val handleSizePx = with(density) { 52.dp.toPx() }
    val maxOffset = if (trackWidth > 0) trackWidth - handleSizePx - with(density) { 12.dp.toPx() } else 0f

    val animatedOffset by animateFloatAsState(targetValue = handleOffset, label = "offset")
    val textAlpha by animateFloatAsState(
        targetValue = if (maxOffset > 0) 1f - (animatedOffset / maxOffset) else 1f,
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp) // 16 * 4
            .shadow(14.dp, RoundedCornerShape(percent = 50), ambientColor = trackColor, spotColor = trackColor)
            .background(trackColor, RoundedCornerShape(percent = 50))
            .padding(6.dp)
            .onSizeChanged { size ->
                trackWidth = size.width
            },
        contentAlignment = Alignment.CenterStart
    ) {
        // Background Text
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                color = textColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.alpha(0.6f * textAlpha)
            )
        }

        // Draggable Handle
        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .size(52.dp)
                .background(handleColor, RoundedCornerShape(percent = 50))
                .shadow(4.dp, RoundedCornerShape(percent = 50))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = {
                            if (handleOffset > maxOffset * 0.8f) {
                                handleOffset = maxOffset
                                if (!isConfirmed) {
                                    isConfirmed = true
                                    onConfirm()
                                }
                            } else {
                                handleOffset = 0f
                            }
                        },
                        onDragCancel = {
                            handleOffset = 0f
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        if (!isConfirmed && maxOffset > 0) {
                            val newOffset = (handleOffset + dragAmount.x).coerceIn(0f, maxOffset)
                            handleOffset = newOffset
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Swipe",
                tint = iconColor,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}
