package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Address
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AddressListUiState
import com.example.foodapp.ui.state.AddressViewModel
import com.example.foodapp.ui.state.AuthState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    onNavigateToMapPicker: () -> Unit,
    onAddressSelected: (Address) -> Unit, // Useful if called from Checkout
    viewModel: AddressViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(user) {
        if (user != null) {
            viewModel.loadAddresses(user.uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Saved Addresses", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        },
        containerColor = VAL_BACKGROUND,
        bottomBar = {
            Surface(color = VAL_BACKGROUND, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                PrimaryButton(
                    onClick = onNavigateToMapPicker,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Address")
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (user == null) {
                Text(
                    text = "Please log in to manage addresses.",
                    modifier = Modifier.align(Alignment.Center),
                    color = TextSecondary
                )
                return@Box
            }

            when (val state = uiState) {
                is AddressListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VAL_BRAND_PRIMARY
                    )
                }
                is AddressListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = com.example.foodapp.theme.ErrorRed,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is AddressListUiState.Success -> {
                    if (state.addresses.isEmpty()) {
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = com.ahad.riderapp.R.drawable.delivery_intercept_illustration),
                                contentDescription = "Delivery Route",
                                modifier = Modifier.size(160.dp).padding(bottom = 16.dp),
                                contentScale = androidx.compose.ui.layout.ContentScale.Fit
                            )
                            Text("No saved delivery addresses", style = MaterialTheme.typography.titleMedium, color = TextSecondary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.addresses) { address ->
                                AddressCard(
                                    address = address,
                                    onClick = { onAddressSelected(address) },
                                    onEdit = { /* Navigate to Edit */ },
                                    onMakeDefault = { viewModel.saveAddress(user.uid, address.copy(isDefault = true)) {} },
                                    onDelete = { viewModel.deleteAddress(user.uid, address.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AddressCard(
    address: Address, 
    onClick: () -> Unit,
    onEdit: () -> Unit = {},
    onMakeDefault: () -> Unit = {},
    onDelete: () -> Unit = {}
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            // Icon based on label
            val icon = when (address.label.lowercase()) {
                "home" -> Icons.Default.Home
                "work" -> Icons.Default.Work
                else -> Icons.Default.LocationOn
            }
            
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(VAL_BACKGROUND, shape = RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = address.label, tint = VAL_BRAND_PRIMARY)
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = address.label,
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    if (address.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            color = VAL_BRAND_PRIMARY.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = VAL_BRAND_PRIMARY,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address.streetAddress,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
                if (address.deliveryInstructions.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Note: ${address.deliveryInstructions}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
            }
            
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { 
                            menuExpanded = false
                            onEdit()
                        }
                    )
                    if (!address.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Make Default") },
                            onClick = { 
                                menuExpanded = false
                                onMakeDefault()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = com.example.foodapp.theme.ErrorRed) },
                        onClick = { 
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
