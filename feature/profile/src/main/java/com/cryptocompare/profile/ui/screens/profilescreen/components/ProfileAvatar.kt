package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import com.cryptocompare.profile.R
import com.cryptocompare.profile.util.ProfileConstants
import com.cryptocompare.ui.theme.Dimensions

/**
 * Инициал лежит под фотографией, а не вместо неё: если картинка не загрузилась,
 * кружок не остаётся пустым.
 */
@Composable
internal fun ProfileAvatar(
    photoUrl: String?,
    displayName: String?,
    email: String?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(Dimensions.Avatar.xl)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = avatarInitial(displayName, email),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )

        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = stringResource(R.string.profile_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(Dimensions.Avatar.xl),
            )
        }
    }
}

private fun avatarInitial(
    displayName: String?,
    email: String?,
): String =
    listOfNotNull(displayName, email)
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        ?.take(1)
        ?.uppercase()
        ?: ProfileConstants.Avatar.FALLBACK_INITIAL
