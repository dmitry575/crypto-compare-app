package com.cryptocompare.helpers.util

/** Имена коллекций и полей в Firestore. */
object FirestoreConstants {
    const val USERS_COLLECTION = "users"
    const val FAVORITES_COLLECTION = "favorites"
    const val TICKER_FIELD = "ticker"

    /**
     * Документ избранного называется id символа. Документы без этого поля —
     * наследство версии, где избранное хранилось по тикеру: их id и есть тикер,
     * и синхронизация разворачивает их в символы.
     */
    const val SYMBOL_ID_FIELD = "symbolId"
    const val UPDATED_AT_FIELD = "updatedAt"
}
