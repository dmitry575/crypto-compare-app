package com.cryptocompare.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import com.cryptocompare.ui.theme.Dimensions

/**
 * Горизонтальная лента чипов.
 *
 * Одна на весь проект, чтобы шаг между капсулами и поведение прокрутки не
 * разъезжались между экранами. Лента, а не ряд равных сегментов: подписи у
 * фильтров разной длины, и растягивать «Все» до ширины «Избранного» незачем,
 * а при крупном системном шрифте ряд просто прокручивается дальше.
 *
 * Край, за которым лента продолжается, затухает. Без этого обрезанный чип
 * выглядел не как «дальше прокрутка», а как наложение: в каталоге при шрифте 1.3
 * «Название» обрывался ровно о прижатый справа чип избранного и будто уходил под
 * него. Затухание рисуется только там, куда действительно можно прокрутить.
 */
@Composable
fun AppChipRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val scrollState = rememberScrollState()

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                // до horizontalScroll: затухание рисуется по краям видимой части,
                // а не по краям прокручиваемого содержимого
                .fadingEdges(scrollState)
                .horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(Dimensions.Gap.sm),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

private fun Modifier.fadingEdges(scrollState: ScrollState): Modifier =
    graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()

            val fade =
                Dimensions.Spacing.lg
                    .toPx()
                    .coerceAtMost(size.width / 2)
            if (scrollState.canScrollBackward) {
                drawRect(
                    brush = Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = 0f, endX = fade),
                    size = Size(fade, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
            if (scrollState.canScrollForward) {
                drawRect(
                    brush =
                        Brush.horizontalGradient(
                            listOf(Color.Black, Color.Transparent),
                            startX = size.width - fade,
                            endX = size.width,
                        ),
                    topLeft = Offset(size.width - fade, 0f),
                    size = Size(fade, size.height),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
