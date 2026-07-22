package com.cryptocompare.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

/**
 * Четыре радиуса вместо двенадцати в [Dimensions]. Больше и не нужно: мелкое
 * (плашки), обычное (поля, чипы), крупное (карточки), очень крупное (диалоги).
 * Всё остальное — либо круг, либо капсула, и задаётся на месте.
 */
val CryptoShapes =
    Shapes(
        extraSmall = RoundedCornerShape(Dimensions.Radius.sm),
        small = RoundedCornerShape(Dimensions.Radius.md),
        medium = RoundedCornerShape(Dimensions.Radius.lg),
        large = RoundedCornerShape(Dimensions.Radius.xl),
        extraLarge = RoundedCornerShape(Dimensions.Radius.xl),
    )
