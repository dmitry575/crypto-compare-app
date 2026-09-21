package com.cryptocompare.data.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import java.io.IOException

/**
 * Настройки из DataStore, где нечитаемый файл — это пустые настройки, а не
 * исключение.
 *
 * `DataStore.data` бросает [IOException] прямо в коллектор: файл повреждён,
 * диск переполнен, прав не хватило. Тема, язык и настройки рынка собираются в
 * `viewModelScope.launch`, где такое исключение никто не ловит, — то есть порча
 * одного файла настроек роняла приложение при запуске, и починить это
 * пользователь мог только переустановкой.
 *
 * Пустые настройки означают «значений нет», и каждый репозиторий уже умеет
 * откатываться на своё значение по умолчанию. Всё, что не [IOException], —
 * не про чтение файла и пробрасывается дальше.
 */
fun DataStore<Preferences>.preferencesOrEmpty(): Flow<Preferences> =
    data.catch { error ->
        if (error is IOException) emit(emptyPreferences()) else throw error
    }
