package com.cryptocompare.portfolio.ui.screens.positioneditscreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.inputBackground
import com.cryptocompare.ui.theme.inputBorder
import com.cryptocompare.ui.theme.textPrimary
import com.cryptocompare.ui.theme.textTertiary

/**
 * Поле выбора биржи в форме позиции: выглядит как соседние поля ввода, но
 * открывает шторку со списком, а не клавиатуру.
 *
 * [label] — подпись поля для TalkBack: подпись над полем на экране — отдельный
 * текст, и без неё поле читалось бы просто «binance, кнопка».
 */
@Composable
internal fun ExchangeField(
    label: String,
    value: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(Dimensions.Radius.input)

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = Dimensions.Height.input)
                .clip(shape)
                .background(MaterialTheme.colorScheme.inputBackground)
                .border(Dimensions.Border.input, MaterialTheme.colorScheme.inputBorder, shape)
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "$label: $value" }
                .padding(horizontal = Dimensions.Padding.inputHorizontal),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.textTertiary,
            modifier = Modifier.size(Dimensions.IconSize.md),
        )
    }
}
