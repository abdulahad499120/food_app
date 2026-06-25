package com.example.foodapp.ui.screens

import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.data.models.Address
import com.example.foodapp.theme.VAL_BRAND_PRIMARY

@Composable
fun SavedAddressesSheet(
    addresses: List<Address>,
    onAddressSelected: (Address) -> Unit,
    onAddNewClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            // Add padding to handle navigation bar / insets gracefully
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(16.dp)
    ) {
        Text(
            text = "Select a Delivery Address",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth()
        ) {
            items(addresses) { address ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onAddressSelected(address) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = VAL_BRAND_PRIMARY,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = address.label,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = address.streetAddress,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onAddNewClick() }
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AddLocation,
                        contentDescription = "Add New",
                        tint = VAL_BRAND_PRIMARY,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Add new address",
                        fontWeight = FontWeight.Bold,
                        color = VAL_BRAND_PRIMARY,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
