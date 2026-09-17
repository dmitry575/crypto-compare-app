package com.cryptocompare.domain.usecase.pairs

import com.cryptocompare.domain.repository.CryptoCompareRepository
import javax.inject.Inject

class GetCatalogLastUpdateUseCase
    @Inject
    constructor(
        private val cryptoCompareRepository: CryptoCompareRepository,
    ) {
        /**
         * Отправная точка для «обновлено в 13:48» на холодном старте: дальше время
         * ведёт экран — по тикам сокета и по догонкам через REST, а они в базе
         * времени не оставляют.
         *
         * `null` — каталога в базе ещё нет, и сказать нечего.
         */
        suspend operator fun invoke(): Long? = cryptoCompareRepository.getCatalogLastUpdate().takeIf { it > 0 }
    }
