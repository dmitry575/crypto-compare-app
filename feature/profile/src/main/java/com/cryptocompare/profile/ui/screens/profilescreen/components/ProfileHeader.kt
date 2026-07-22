package com.cryptocompare.profile.ui.screens.profilescreen.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.model.auth.AuthUser
import com.cryptocompare.profile.R
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary

@Composable
internal fun ProfileHeader(
    user: AuthUser,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
    ) {
        ProfileAvatar(
            photoUrl = user.photoUrl,
            displayName = user.displayName,
            email = user.email,
        )

        user.displayName?.takeIf { it.isNotBlank() }?.let { name ->
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Text(
            text = user.email?.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_no_email),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
