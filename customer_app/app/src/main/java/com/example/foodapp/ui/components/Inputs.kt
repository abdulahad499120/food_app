package com.example.foodapp.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.DividerColor
import com.example.foodapp.theme.ErrorRed
import com.example.foodapp.theme.FoodAppTheme
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    errorMessage: String? = null,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default
) {
    val isError = errorMessage != null

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isError) ErrorRed else TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(text = placeholder, color = TextSecondary)
            },
            isError = isError,
            shape = RoundedCornerShape(8.dp),
            colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = DividerColor,
                errorBorderColor = ErrorRed,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            singleLine = true,
            keyboardOptions = keyboardOptions
        )
        if (isError) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage!!,
                style = MaterialTheme.typography.labelSmall,
                color = ErrorRed
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TextInputPreview() {
    FoodAppTheme {
        Surface(modifier = Modifier.padding(16.dp)) {
            Column {
                TextInput(
                    label = "Email Address",
                    value = "saeed@example.com",
                    onValueChange = {}
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextInput(
                    label = "Password",
                    value = "",
                    placeholder = "Enter your password",
                    onValueChange = {},
                    errorMessage = "Password is required"
                )
            }
        }
    }
}
