package com.cryptocompare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.IOException

/**
 * DataStore, который не может прочитать свой файл.
 *
 * Так ведёт себя настоящий при повреждённом файле настроек, переполненном
 * диске или отобранных правах: [IOException] прилетает прямо в коллектор.
 */
internal class BrokenPreferencesDataStore : DataStore<Preferences> {
    override val data: Flow<Preferences> = flow { throw IOException("preferences file is corrupted") }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences =
        throw IOException("preferences file is corrupted")
}
