package com.example.foodapp.ui.screens

import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_ON_PRIMARY
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VAL_BACKGROUND,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        modifier = modifier
    ) {


    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cross icon at top right
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            IconButton(onClick = onDismiss, modifier = Modifier.bounceClick { onDismiss() }) {
                Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = VAL_SURFACE_DARK)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = VAL_SURFACE_DARK,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Experience the best food delivery app, right at your fingertips. Log in to your account or create a new one to get started.",
            style = MaterialTheme.typography.bodyMedium,
            color = VAL_SURFACE_DARK.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = VAL_BRAND_PRIMARY,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick { onNavigateToSignIn() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Sign In", color = VAL_BRAND_ON_PRIMARY, style = MaterialTheme.typography.labelLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = androidx.compose.ui.graphics.Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, VAL_BRAND_PRIMARY),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick { onNavigateToSignUp() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Join Now", color = VAL_BRAND_PRIMARY, style = MaterialTheme.typography.labelLarge)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
    }
}
