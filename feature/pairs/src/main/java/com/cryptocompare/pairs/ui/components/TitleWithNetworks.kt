package com.cryptocompare.pairs.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import com.cryptocompare.helpers.util.NetworkConstants
import com.cryptocompare.ui.theme.textTertiary

/**
 * Заголовок экрана пары и сети символа под ним.
 *
 * Сети здесь полным списком, а не короткой меткой каталога: на экране пары место
 * есть, а по одной метке «Ethereum, BNB Chain +5» два символа ETHUSDC бывают
 * неотличимы — у обоих Ethereum и BNB Chain.
 */
@Composable
internal fun TitleWithNetworks(
    title: String,
    networks: List<String>,
    titleStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            style = titleStyle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (networks.isNotEmpty()) {
            Text(
                text = networks.joinToString(NetworkConstants.LABEL_JOINER),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
