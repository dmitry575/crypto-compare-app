package com.cryptocompare.auth.ui.screens.onboardingscreen.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cryptocompare.auth.util.AuthConstants
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.borderPrimary

/** Точки-индикаторы: текущая вытягивается в капсулу, остальные приглушены. */
@Composable
internal fun OnboardingIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(pageCount) { page ->
            val isCurrent = page == currentPage
            val width by animateDpAsState(
                targetValue =
                    if (isCurrent) {
                        AuthConstants.Onboarding.dotWidthActive
                    } else {
                        AuthConstants.Onboarding.dotSize
                    },
                label = "dotWidth",
            )
            val color by animateColorAsState(
                targetValue =
                    if (isCurrent) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.borderPrimary
                    },
                label = "dotColor",
            )

            Row(
                modifier =
                    Modifier
                        .width(width)
                        .height(AuthConstants.Onboarding.dotSize)
                        .background(color, RoundedCornerShape(Dimensions.Radius.full)),
                content = {},
            )
        }
    }
}
