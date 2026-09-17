package com.cryptocompare.pairs.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.StreamStatus
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.statusActive
import com.cryptocompare.ui.theme.statusInactive
import com.cryptocompare.ui.theme.textSecondary

/**
 * Живые ли цены на экране.
 *
 * Пока поток жив, подписи нет — только точка: строка «Онлайн» в шапке у каждого
 * запуска приложения означала бы ровно ничего. Слова появляются там, где числам
 * верить нельзя, и состояние читается, даже если смысл точки забыт. Для
 * скринридера состояние названо всегда.
 *
 * «Связь восстанавливается» показана кружком ожидания, а не третьим цветом:
 * зелёный и красный в приложении заняты направлением цены, а акцент — заметным
 * спредом.
 */
@Composable
internal fun StreamStatusBadge(
    status: StreamStatus,
    modifier: Modifier = Modifier,
) {
    val description =
        stringResource(
            when (status) {
                StreamStatus.LIVE -> R.string.pairs_stream_live
                StreamStatus.RECONNECTING -> R.string.pairs_stream_reconnecting
                StreamStatus.OFFLINE -> R.string.pairs_stream_offline
            },
        )

    Row(
        modifier = modifier.semantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.xs),
    ) {
        if (status == StreamStatus.RECONNECTING) {
            CircularProgressIndicator(
                modifier = Modifier.size(PairsConstants.MainScreen.STREAM_PROGRESS_SIZE),
                strokeWidth = PairsConstants.MainScreen.STREAM_PROGRESS_STROKE,
                color = MaterialTheme.colorScheme.textSecondary,
            )
        } else {
            Icon(
                imageVector = Icons.Filled.Circle,
                contentDescription = null,
                tint =
                    if (status == StreamStatus.LIVE) {
                        MaterialTheme.colorScheme.statusActive
                    } else {
                        MaterialTheme.colorScheme.statusInactive
                    },
                modifier = Modifier.size(PairsConstants.MainScreen.STREAM_DOT_SIZE),
            )
        }

        if (status != StreamStatus.LIVE) {
            Text(
                text = description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.textSecondary,
            )
        }
    }
}
