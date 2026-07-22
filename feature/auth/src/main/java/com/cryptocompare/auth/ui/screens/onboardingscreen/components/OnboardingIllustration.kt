package com.cryptocompare.auth.ui.screens.onboardingscreen.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.cryptocompare.auth.ui.screens.onboardingscreen.OnboardingPage
import com.cryptocompare.auth.util.AuthConstants

/**
 * Иллюстрация слайда. Каждая собрана из элементов настоящего интерфейса теми же
 * токенами: через минуту пользователь встретит их в приложении и узнает.
 *
 * Высота фиксирована, чтобы заголовки на всех слайдах стояли на одной линии
 * и не прыгали при пролистывании.
 */
@Composable
internal fun OnboardingIllustration(
    page: OnboardingPage,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(AuthConstants.Onboarding.artHeight),
        contentAlignment = Alignment.Center,
    ) {
        when (page) {
            OnboardingPage.Compare -> ExchangeRowsArt()
            OnboardingPage.Spread -> SpreadTrackArt()
            OnboardingPage.Follow -> CandlesArt()
        }
    }
}
