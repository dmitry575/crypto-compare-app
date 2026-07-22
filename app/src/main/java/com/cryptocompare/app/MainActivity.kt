package com.cryptocompare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.cryptocompare.app.navigation.AppNavigation
import com.cryptocompare.app.viewmodel.ThemeViewModel
import com.cryptocompare.helpers.isDark
import com.cryptocompare.ui.theme.CryptoCompareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val themeViewModel: ThemeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val preference by themeViewModel.themePreference.collectAsState()

            CryptoCompareTheme(darkTheme = preference.isDark(isSystemInDarkTheme())) {
                AppNavigation()
            }
        }
    }
}
