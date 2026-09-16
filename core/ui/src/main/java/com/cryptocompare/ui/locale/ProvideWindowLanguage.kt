package com.cryptocompare.ui.locale

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext

/**
 * Возвращает язык приложения внутрь диалога или шторки.
 *
 * Диалоги и шторки Compose живут в собственном окне со своим `AndroidComposeView`,
 * а тот заново раздаёт [LocalContext] и [LocalConfiguration] — от активности, без
 * подмены из [ProvideAppLanguage]. Строку, прочитанную уже внутри окна,
 * `stringResource` берёт из системной локали: в русском интерфейсе шторка
 * сортировки была английской целиком, а у диалога выхода английской была кнопка
 * «Cancel».
 *
 * [outerContext] читается **снаружи** окна — там [LocalContext] ещё
 * локализованный:
 * ```
 * val outerContext = LocalContext.current
 *
 * ModalBottomSheet(onDismissRequest = onDismiss) {
 *     ProvideWindowLanguage(outerContext) { SheetContent() }
 * }
 * ```
 *
 * Строки, которые экран резолвит до открытия окна и передаёт готовыми, в обёртке
 * не нуждаются — они уже на нужном языке.
 */
@Composable
fun ProvideWindowLanguage(
    outerContext: Context,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalContext provides outerContext,
        LocalConfiguration provides outerContext.resources.configuration,
        content = content,
    )
}
