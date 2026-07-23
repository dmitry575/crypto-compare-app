package com.cryptocompare.domain.usecase.onboarding

import com.cryptocompare.domain.repository.OnboardingRepository
import javax.inject.Inject

class HasSeenOnboardingUseCase
    @Inject
    constructor(
        private val onboardingRepository: OnboardingRepository,
    ) {
        suspend operator fun invoke(): Boolean = onboardingRepository.hasSeenOnboarding()
    }
