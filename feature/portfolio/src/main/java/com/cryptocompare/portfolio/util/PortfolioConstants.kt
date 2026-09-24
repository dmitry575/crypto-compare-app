package com.cryptocompare.portfolio.util

/** Константы портфеля: экран, навигация и разбор ввода. */
object PortfolioConstants {
    object Screen {
        /** Ключ карточки итога в списке: позицией она не является, и symbolId у неё нет. */
        const val SUMMARY_KEY = "summary"

        /** Ключ полоски «цены не обновляются»: она тоже не позиция. */
        const val STALE_NOTICE_KEY = "stale"
    }

    /** Имена аргументов — это имена полей `PortfolioRoute.PositionEdit`: ViewModel читают по ним `SavedStateHandle`. */
    object Navigation {
        const val SYMBOL_ID_ARG = "symbolId"
        const val TICKER_ARG = "ticker"

        /** Цена, с которой форма откроется для новой позиции: текущий ask выбранной биржи. */
        const val PRICE_ARG = "price"

        /** Биржа, с которой форма откроется для новой позиции: выбранная на экране пары. */
        const val PROVIDER_ID_ARG = "providerId"
    }

    object Input {
        /** Разделители разрядов, которые люди вставляют руками и копируют с бирж. */
        val GROUPING_CHARS = setOf(' ', ' ', ' ', '_')
        const val DECIMAL_COMMA = ','
        const val DECIMAL_DOT = '.'
    }
}
