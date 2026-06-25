package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.state.ChatViewModel
import com.example.foodapp.ui.state.LiveTrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RiderChatScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    chatViewModel: ChatViewModel,
    liveTrackingViewModel: LiveTrackingViewModel
) {
    val messages by chatViewModel.messages.collectAsStateWithLifecycle()
    val trackingState by liveTrackingViewModel.uiState.collectAsStateWithLifecycle()
    
    var inputText by remember { mutableStateOf("") }
    
    LaunchedEffect(orderId) {
        chatViewModel.initializeChat(orderId)
    }

    val isChatDisabled = trackingState.orderStatus == OrderStatus.DELIVERED || trackingState.orderStatus == OrderStatus.CANCELLED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with Rider") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding()
                        .imePadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { 
                            Text(if (isChatDisabled) "Chat closed" else "Type a message...") 
                        },
                        enabled = !isChatDisabled,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color(0xFFF5F5F5),
                            disabledContainerColor = Color(0xFFE0E0E0)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isChatDisabled) {
                                chatViewModel.sendMessage(orderId, inputText, "CUSTOMER")
                                inputText = ""
                            }
                        },
                        enabled = inputText.isNotBlank() && !isChatDisabled,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (inputText.isNotBlank() && !isChatDisabled) VAL_BRAND_PRIMARY else Color.Gray)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                reverseLayout = true, // Auto-scroll to newest message
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                items(messages) { msg ->
                    val isCustomer = msg.senderType == "CUSTOMER"
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = if (isCustomer) Arrangement.End else Arrangement.Start
                    ) {
                        Box(
                            modifier = Modifier
                                .widthIn(max = 280.dp)
                                .clip(RoundedCornerShape(
                                    topStart = 16.dp,
                                    topEnd = 16.dp,
                                    bottomStart = if (isCustomer) 16.dp else 4.dp,
                                    bottomEnd = if (isCustomer) 4.dp else 16.dp
                                ))
                                .background(if (isCustomer) VAL_BRAND_PRIMARY else Color(0xFFEEEEEE))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = msg.text,
                                color = if (isCustomer) Color.White else TextPrimary,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
