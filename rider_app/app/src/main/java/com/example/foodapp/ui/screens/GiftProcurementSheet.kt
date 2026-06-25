package com.example.foodapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.data.models.GiftTemplate
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BACKGROUND

private val SuccessGreen = Color(0xFF4CAF50)

@Composable
fun GiftProcurementSheet(
    template: GiftTemplate,
    onPayAndCheckout: (name: String, email: String, amount: Int, message: String) -> Unit,
    onDismiss: () -> Unit,
    isSuccess: Boolean = false
) {
    AnimatedContent(
        targetState = isSuccess,
        transitionSpec = {
            (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.9f)) togetherWith fadeOut(animationSpec = tween(400))
        },
        label = "procurement_state"
    ) { success ->
        if (success) {
            GiftSuccessView(onDismiss = onDismiss)
        } else {
            GiftFormView(
                template = template,
                onPayAndCheckout = onPayAndCheckout
            )
        }
    }
}

@Composable
fun GiftSuccessView(onDismiss: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(SuccessGreen.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Success",
                tint = SuccessGreen,
                modifier = Modifier.size(40.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Gift Sent!",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Your eGift is on its way to the recipient.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        TextButton(onClick = { /* Checkbox logic for send copy to yourself */ }) {
            Text("Send a copy to yourself", color = VAL_BRAND_PRIMARY)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY)
        ) {
            Text("Done")
        }
    }
}

@Composable
fun GiftFormView(
    template: GiftTemplate,
    onPayAndCheckout: (name: String, email: String, amount: Int, message: String) -> Unit
) {
    var recipientName by remember { mutableStateOf("") }
    var recipientEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var selectedAmount by remember { mutableStateOf(10) }
    
    val amounts = listOf(5, 10, 25, 50)
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Send eGift",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = recipientName,
            onValueChange = { recipientName = it },
            label = { Text("Recipient Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedTextField(
            value = recipientEmail,
            onValueChange = { recipientEmail = it },
            label = { Text("Recipient Email") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(text = "Amount", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            amounts.forEach { amt ->
                val isSelected = selectedAmount == amt
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) VAL_BRAND_PRIMARY else VAL_BACKGROUND)
                        .clickable { selectedAmount = amt }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$${amt}",
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = message,
            onValueChange = { if (it.length <= 160) message = it },
            label = { Text("Message") },
            modifier = Modifier.fillMaxWidth(),
            maxLines = 3
        )
        
        Text(
            text = "${message.length}/160",
            style = MaterialTheme.typography.labelSmall,
            color = if (message.length == 160) Color.Red else TextSecondary,
            modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { onPayAndCheckout(recipientName, recipientEmail, selectedAmount, message) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY)
            ) {
                Text("Pay & Checkout")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
