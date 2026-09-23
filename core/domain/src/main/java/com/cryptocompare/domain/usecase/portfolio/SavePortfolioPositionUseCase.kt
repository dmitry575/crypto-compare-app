package com.cryptocompare.domain.usecase.portfolio

import com.cryptocompare.domain.repository.PortfolioRepository
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.AppException
import com.cryptocompare.model.error.ValidationErrorReason
import com.cryptocompare.model.portfolio.PortfolioPosition
import com.cryptocompare.model.portfolio.PortfolioPositionDraft
import javax.inject.Inject

class SavePortfolioPositionUseCase
    @Inject
    constructor(
        private val portfolioRepository: PortfolioRepository,
    ) {
        /**
         * Ноль количества — это не позиция, а её отсутствие: форма с нулём удаляет
         * строку, иначе портфель копил бы пустышки, которые нечем закрыть.
         *
         * Отрицательные значения не принимаются: короткие позиции в ручном портфеле
         * не поддерживаются, и молча превратить их в длинные было бы хуже отказа.
         */
        suspend operator fun invoke(draft: PortfolioPositionDraft): Result<Unit> {
            if (draft.amount < 0 || draft.buyPrice < 0) {
                return Result.failure(AppException(AppError.Validation(ValidationErrorReason.NEGATIVE_VALUES)))
            }

            if (draft.amount == 0.0) return portfolioRepository.deletePosition(draft.symbolId)

            return portfolioRepository.savePosition(
                PortfolioPosition(
                    symbolId = draft.symbolId,
                    ticker = draft.ticker,
                    amount = draft.amount,
                    buyPrice = draft.buyPrice,
                    updatedAtMillis = System.currentTimeMillis(),
                    providerId = draft.providerId,
                ),
            )
        }
    }
