package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.borderPrimary

/**
 * Группа настроек одной карточкой. Плоский список строк с иконками не показывал,
 * что относится к оформлению, а что к аккаунту, — граница группы показывает.
 */
@Composable
internal fun ProfileGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Dimensions.Radius.card)

    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .background(MaterialTheme.colorScheme.bgCard)
                .border(
                    width = Dimensions.Border.card,
                    color = MaterialTheme.colorScheme.borderPrimary,
                    shape = shape,
                ),
        content = content,
    )
}
