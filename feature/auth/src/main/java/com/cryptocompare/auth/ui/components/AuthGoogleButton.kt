package com.cryptocompare.auth.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.cryptocompare.auth.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary
import com.cryptocompare.ui.theme.inputBackground
import com.cryptocompare.ui.theme.textPrimary

@Composable
fun AuthGoogleButton(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(Dimensions.Height.button)
                .background(
                    color = MaterialTheme.colorScheme.inputBackground,
                    shape = RoundedCornerShape(Dimensions.Radius.button),
                ).border(
                    BorderStroke(Dimensions.Border.thin, MaterialTheme.colorScheme.borderPrimary),
                    shape = RoundedCornerShape(Dimensions.Radius.button),
                ).clickable(onClick = onClick)
                .padding(horizontal = Dimensions.Padding.inputHorizontal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_google),
            contentDescription = null,
            // фирменные цвета знака не перекрашиваем — это требование Google
            tint = Color.Unspecified,
            modifier = Modifier.size(Dimensions.IconSize.md),
        )

        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.textPrimary,
            modifier = Modifier.padding(start = Dimensions.Spacing.sm),
        )
    }
}
