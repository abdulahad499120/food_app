package com.example.foodapp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.DividerColor
import com.example.foodapp.theme.FoodAppTheme
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextSecondary

@Composable
fun PrimaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = VAL_BRAND_PRIMARY,
            contentColor = SurfaceWhite,
            disabledContainerColor = DividerColor,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        content = content
    )
}

@Composable
fun SecondaryButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (enabled) VAL_BRAND_PRIMARY else DividerColor),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = VAL_BRAND_PRIMARY,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 24.dp),
        content = content
    )
}

@Composable
fun GhostButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        enabled = enabled,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.textButtonColors(
            contentColor = VAL_BRAND_PRIMARY,
            disabledContentColor = TextSecondary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp),
        content = content
    )
}

// Previews
@Preview(showBackground = true)
@Composable
fun PrimaryButtonPreview() {
    FoodAppTheme {
        PrimaryButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("Add to Cart • Rs. 450")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SecondaryButtonPreview() {
    FoodAppTheme {
        SecondaryButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
            Text("View Cart")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun GhostButtonPreview() {
    FoodAppTheme {
        GhostButton(onClick = {}) {
            Text("Clear All")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DisabledButtonPreview() {
    FoodAppTheme {
        PrimaryButton(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
            Text("Checkout (Disabled)")
        }
    }
}
