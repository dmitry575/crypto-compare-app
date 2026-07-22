package com.cryptocompare.auth.ui.screens.onboardingscreen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cryptocompare.auth.R
import com.cryptocompare.auth.ui.components.AuthBackground
import com.cryptocompare.auth.ui.screens.onboardingscreen.components.OnboardingIndicator
import com.cryptocompare.auth.ui.screens.onboardingscreen.components.OnboardingSlide
import com.cryptocompare.auth.viewmodel.onboardingviewmodel.OnboardingViewModel
import com.cryptocompare.ui.components.AppPrimaryButton
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.textSecondary
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value
    val pages = OnboardingPage.entries
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    LaunchedEffect(uiState.isFinished) {
        if (uiState.isFinished) onDone()
    }

    AuthBackground {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(vertical = Dimensions.Padding.screenVertical),
        ) {
            // «Пропустить» доступен на каждом слайде, а не только на последнем
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.Padding.screenHorizontal),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = viewModel::onFinish) {
                    Text(
                        text = stringResource(R.string.onboarding_skip),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.textSecondary,
                    )
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val slide = pages[page]
                OnboardingSlide(
                    title = stringResource(slide.titleRes),
                    text = stringResource(slide.textRes),
                )
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimensions.Padding.screenHorizontal),
                verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.xl),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                OnboardingIndicator(
                    pageCount = pages.size,
                    currentPage = pagerState.currentPage,
                )

                val isLastPage = pagerState.currentPage == pages.lastIndex
                AppPrimaryButton(
                    text =
                        stringResource(
                            if (isLastPage) R.string.onboarding_start else R.string.onboarding_next,
                        ),
                    onClick = {
                        if (isLastPage) {
                            viewModel.onFinish()
                        } else {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        }
                    },
                )
            }
        }
    }
}
