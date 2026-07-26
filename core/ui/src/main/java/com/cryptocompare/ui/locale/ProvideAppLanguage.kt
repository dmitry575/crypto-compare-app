package com.cryptocompare.ui.locale

import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.cryptocompare.model.settings.AppLanguage
import java.util.Locale

/**
 * Применяет язык подменой [Configuration], а не через
 * `AppCompatDelegate.setApplicationLocales`: тот пересоздаёт активность, и на время
 * recreate контент пропадал и прыгал вверх-вниз, пока доезжали window insets.
 *
 * Здесь язык меняется как тема — рекомпозицией, без пересоздания. `stringResource`
 * читает `LocalResources`, а тот вычисляется из `LocalContext.resources`, поэтому
 * подмены контекста достаточно, чтобы перевести весь UI разом.
 * [AppLanguage.SYSTEM] оставляет системную локаль — подмену не делаем, но провайдер
 * вызываем всё равно: убирать его из композиции значило бы пересоздавать поддерево.
 */
@Composable
fun ProvideAppLanguage(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current

    val localizedConfiguration =
        remember(configuration, language) {
            if (language == AppLanguage.SYSTEM) {
                configuration
            } else {
                Configuration(configuration).apply {
                    setLocale(Locale.forLanguageTag(language.localeTag))
                }
            }
        }

    val localizedContext =
        remember(context, localizedConfiguration) {
            if (localizedConfiguration === configuration) {
                context
            } else {
                val configured = context.createConfigurationContext(localizedConfiguration)

                // Отдать `configured` напрямую нельзя: это ContextImpl, а не обёртка
                // над активностью. И hiltViewModel(), и findActivity() ищут Activity
                // обходом baseContext — на голом ContextImpl цикл не выполняется ни
                // разу и падает «Expected an activity context». Поэтому наружу идёт
                // обёртка над самой активностью, у которой подменены только ресурсы;
                // тему не трогаем, иначе у контекста окажется тема по умолчанию.
                object : ContextWrapper(context) {
                    override fun getResources(): Resources = configured.resources
                }
            }
        }

    CompositionLocalProvider(
        LocalConfiguration provides localizedConfiguration,
        LocalContext provides localizedContext,
        content = content,
    )
}
