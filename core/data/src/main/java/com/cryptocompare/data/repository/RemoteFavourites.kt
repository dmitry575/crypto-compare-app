package com.cryptocompare.data.repository

import com.cryptocompare.data.local.entity.FavouriteSymbolEntity
import com.google.firebase.firestore.DocumentReference

/**
 * Избранное, прочитанное из Firestore, и документы старого формата, которые
 * удалось развернуть в символы: их удаляют после того, как развёрнутое уехало
 * обратно новыми документами.
 */
internal data class RemoteFavourites(
    val favourites: Map<Long, FavouriteSymbolEntity>,
    val legacyDocs: List<DocumentReference>,
)
