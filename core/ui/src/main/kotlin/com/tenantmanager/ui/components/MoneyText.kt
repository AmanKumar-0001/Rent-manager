package com.tenantmanager.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import com.tenantmanager.domain.model.Money
import com.tenantmanager.ui.util.Formatters

@Composable
fun MoneyText(
    amount: Money,
    modifier: Modifier = Modifier,
    symbol: String = "₹",
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null
) {
    val formatted = Formatters.formatCurrency(amount, symbol)

    Text(
        text = formatted,
        modifier = modifier.semantics {
            contentDescription = "Amount: $formatted"
        },
        style = style.copy(
            fontFeatureSettings = "tnum",
            fontWeight = fontWeight ?: style.fontWeight
        ),
        color = color
    )
}

