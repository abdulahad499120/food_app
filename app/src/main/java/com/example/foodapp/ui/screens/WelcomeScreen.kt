package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_ON_PRIMARY
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK

@Composable
fun WelcomeScreen(
    onNavigateToSignIn: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VAL_BACKGROUND)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.weight(1f))
        
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = VAL_SURFACE_DARK,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Experience the best food delivery app, right at your fingertips. Log in to your account or create a new one to get started.",
            style = MaterialTheme.typography.bodyLarge,
            color = VAL_SURFACE_DARK.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNavigateToSignIn,
            colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Sign In", color = VAL_BRAND_ON_PRIMARY, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onNavigateToSignUp,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Text("Join Now", color = VAL_BRAND_PRIMARY, fontSize = MaterialTheme.typography.titleMedium.fontSize)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
