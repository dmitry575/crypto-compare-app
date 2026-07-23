package com.cryptocompare.auth.ui.screens.onboardingscreen

import androidx.annotation.StringRes
import com.cryptocompare.auth.R

/**
 * Слайды онбординга. Порядок объявления — порядок показа, поэтому список
 * страниц и их количество берутся отсюда, а не задаются в разметке отдельно.
 */
internal enum class OnboardingPage(
    @StringRes val titleRes: Int,
    @StringRes val textRes: Int,
) {
    Compare(R.string.onboarding_slide_1_title, R.string.onboarding_slide_1_text),
    Spread(R.string.onboarding_slide_2_title, R.string.onboarding_slide_2_text),
    Follow(R.string.onboarding_slide_3_title, R.string.onboarding_slide_3_text),
}
