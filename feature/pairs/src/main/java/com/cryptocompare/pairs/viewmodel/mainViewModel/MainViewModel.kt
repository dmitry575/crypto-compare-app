package com.cryptocompare.pairs.viewmodel.mainViewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.cryptocompare.domain.usecase.auth.GetCurrentUserUseCase
import com.cryptocompare.domain.usecase.auth.ObserveAuthStateUseCase
import com.cryptocompare.domain.usecase.pairs.ApplyBestPriceChangesUseCase
import com.cryptocompare.domain.usecase.pairs.GetCatalogLastUpdateUseCase
import com.cryptocompare.domain.usecase.pairs.LoadPairsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveConnectionStateUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveFavouriteSymbolsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveStreamReconnectsUseCase
import com.cryptocompare.domain.usecase.pairs.ObserveTickerEventUseCase
import com.cryptocompare.domain.usecase.pairs.RefreshBestPricesUseCase
import com.cryptocompare.domain.usecase.pairs.StreamDisconnectUseCase
import com.cryptocompare.domain.usecase.pairs.SyncFavouriteSymbolsUseCase
import com.cryptocompare.domain.usecase.pairs.SyncVisibleTickersUseCase
import com.cryptocompare.domain.usecase.pairs.ToggleFavouriteSymbolUseCase
import com.cryptocompare.model.error.AppError
import com.cryptocompare.model.error.asAppError
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.model.symbol.CatalogSort
import com.cryptocompare.model.symbol.CatalogSorting
import com.cryptocompare.model.symbol.PairUiItem
import com.cryptocompare.model.ticker.TickerBestPrice
import com.cryptocompare.model.ticker.TickerStreamEvent
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.util.StreamStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration.Companion.milliseconds

@HiltViewModel
class MainViewModel
    @Inject
    constructor(
        private val loadPairsUseCase: LoadPairsUseCase,
        private val syncVisibleTickersUseCase: SyncVisibleTickersUseCase,
        private val streamDisconnectUseCase: StreamDisconnectUseCase,
        private val observeTickerEventUseCase: ObserveTickerEventUseCase,
        private val applyBestPriceChangesUseCase: ApplyBestPriceChangesUseCase,
        private val observeFavouriteSymbolsUseCase: ObserveFavouriteSymbolsUseCase,
        private val syncFavouriteSymbolsUseCase: SyncFavouriteSymbolsUseCase,
        private val toggleFavouriteSymbolUseCase: ToggleFavouriteSymbolUseCase,
        private val observeStreamReconnectsUseCase: ObserveStreamReconnectsUseCase,
        private val refreshBestPricesUseCase: RefreshBestPricesUseCase,
        private val observeConnectionStateUseCase: ObserveConnectionStateUseCase,
        private val getCatalogLastUpdateUseCase: GetCatalogLastUpdateUseCase,
        private val observeAuthStateUseCase: ObserveAuthStateUseCase,
        private val getCurrentUserUseCase: GetCurrentUserUseCase,
    ) : ViewModel() {
        private val _uiState = MutableStateFlow(MainUiState())
        val uiState = _uiState.asStateFlow()

        private val subscribedTickers = mutableSetOf<String>()

        // Лучшие пары цен из сокета копятся здесь и уходят в базу пачками, чтобы
        // UI не перерисовывался на каждый тик. Ключ — symbolId, он у тикера один,
        // и это ровно та причина, по которой сюда нельзя пускать событие типа 4:
        // котировки всех бирж легли бы под один ключ, и в каталог попадала бы
        // последняя тикнувшая биржа вместо разницы между биржами.
        private val pendingPriceUpdates = mutableMapOf<Long, TickerBestPrice>()
        private val pendingPricesLock = Any()

        // guarded by pendingPricesLock; cleared in the SAME critical section that
        // observes an empty queue, so a tick can't slip in between "queue is empty"
        // and the job ending and get stranded with no flush scheduled for it
        private var isFlushScheduled = false

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        val pairs: Flow<PagingData<PairUiItem>> =
            combine(
                _uiState
                    .map { it.searchQuery }
                    .distinctUntilChanged()
                    .debounce { query -> if (query.isEmpty()) 0L else PairsConstants.MainScreen.SEARCH_DEBOUNCE_MS },
                _uiState.map { it.onlyFavourite }.distinctUntilChanged(),
                _uiState.map { it.favouriteSymbolIds }.distinctUntilChanged(),
                _uiState.map { it.direction }.distinctUntilChanged(),
                _uiState.map { it.sorting }.distinctUntilChanged(),
            ) { query, onlyFavourite, favourites, direction, sorting ->
                PairsFilter(
                    query = query,
                    onlyFavourite = onlyFavourite,
                    // the favourites set matters for the query only when the filter is
                    // on; dropping it otherwise keeps star taps from rebuilding the pager
                    favouriteSymbolIds = if (onlyFavourite) favourites else emptySet(),
                    direction = direction,
                    sorting = sorting,
                )
            }.distinctUntilChanged()
                .flatMapLatest { filter ->
                    loadPairsUseCase(
                        query = filter.query,
                        onlyFavourite = filter.onlyFavourite,
                        favouriteSymbolIds = filter.favouriteSymbolIds,
                        direction = filter.direction,
                        sorting = filter.sorting,
                    )
                }.cachedIn(viewModelScope)

        init {
            observeSocket()
            observeConnectionState()
            observeStaleStream()
            loadLastUpdate()
            observeReconnects()
            observeAuthState()
            syncFavouriteSymbols()
            observeFavouriteSymbols()
        }

        fun onSearchQueryChange(query: String) {
            _uiState.update { it.copy(searchQuery = query) }
        }

        fun onFavouriteClick(
            symbolId: Long,
            ticker: String,
        ) {
            if (isGuest()) {
                requireSignIn()
                return
            }

            viewModelScope.launch {
                toggleFavouriteSymbolUseCase(symbolId, ticker).onFailure { exception ->
                    _uiState.update { it.copy(error = exception.asAppError()) }
                }
            }
        }

        fun onOnlyFavouriteChange(enabled: Boolean) {
            // у гостя избранного нет вовсе: пустой список вместо объяснения выглядел
            // бы поломкой
            if (enabled && isGuest()) {
                requireSignIn()
                return
            }

            _uiState.update { it.copy(onlyFavourite = enabled) }
        }

        fun onSignInRequestShown() {
            _uiState.update { it.copy(signInRequired = false) }
        }

        /**
         * Спрашиваем текущего пользователя, а не флаг из состояния: поток входа
         * приезжает асинхронно, и звезда, нажатая в первые миллисекунды после
         * запуска, у вошедшего пользователя просила бы вход.
         */
        private fun isGuest(): Boolean = runCatching { getCurrentUserUseCase() }.getOrNull() == null

        private fun requireSignIn() {
            _uiState.update { it.copy(signInRequired = true) }
        }

        fun onDirectionChange(direction: CatalogDirection) {
            _uiState.update { it.copy(direction = direction) }
        }

        /**
         * Повторный выбор того же поля переворачивает порядок — так работает
         * сортировка везде, где по заголовку кликают дважды.
         *
         * Новое поле берёт направление по умолчанию: имя по алфавиту, всё
         * остальное — по убыванию. «Сортировать по объёму» означает «покажи
         * самые крупные», а не «покажи мёртвые пары первыми».
         */
        fun onSortSelected(field: CatalogSort) {
            _uiState.update { state ->
                val current = state.sorting
                val next =
                    if (current.field == field) {
                        current.copy(ascending = !current.ascending)
                    } else {
                        CatalogSorting(field = field, ascending = field == CatalogSort.NAME)
                    }

                state.copy(sorting = next)
            }
        }

        fun onVisibleTickersChange(visibleTickers: List<String>) {
            val updatedSubscribedTickers = syncVisibleTickersUseCase(visibleTickers, subscribedTickers)

            subscribedTickers.clear()
            subscribedTickers.addAll(updatedSubscribedTickers)

            _uiState.update { it.copy(subscribedTickers = updatedSubscribedTickers) }
        }

        /**
         * «Обновить» на полоске устаревших цен: сокет ещё переподключается с
         * бэкоффом, а цены видимых строк можно взять по REST прямо сейчас.
         */
        fun onRefreshClick() {
            viewModelScope.launch {
                val visibleTickers = subscribedTickers.toSet()

                refreshBestPricesUseCase(visibleTickers)
                    .onSuccess { updated ->
                        // ноль обновлений при непустом списке — это «ни один запрос не
                        // прошёл»: use case глотает ошибки по отдельным тикерам, и
                        // молча оставить время нетронутым значило бы сделать вид,
                        // что кнопка сработала
                        if (updated > 0) {
                            markUpdated()
                        } else if (visibleTickers.isNotEmpty()) {
                            _uiState.update { it.copy(refreshFailed = true) }
                        }
                    }.onFailure { exception -> _uiState.update { it.copy(error = exception.asAppError()) } }
            }
        }

        fun onRefreshFailureShown() {
            _uiState.update { it.copy(refreshFailed = false) }
        }

        fun onErrorShown() {
            _uiState.update { it.copy(error = null) }
        }

        private fun observeSocket() {
            viewModelScope.launch {
                try {
                    observeTickerEventUseCase().collect { event ->
                        if (event is TickerStreamEvent.TickerBestPriceChange) {
                            synchronized(pendingPricesLock) {
                                pendingPriceUpdates[event.data.symbolId] = event.data
                            }
                            schedulePriceFlush()
                        }
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // поток котировок — не запрос: что бы ни сломалось внутри,
                    // для пользователя это «поток цен прервался»
                    _uiState.update { it.copy(error = AppError.Stream) }
                }
            }
        }

        private fun markUpdated() {
            _uiState.update { it.copy(lastUpdateMillis = System.currentTimeMillis()) }
        }

        /** Отправная точка для «обновлено в 13:48», когда приложение открыли без сети. */
        private fun loadLastUpdate() {
            viewModelScope.launch {
                val lastUpdate = runCatching { getCatalogLastUpdateUseCase() }.getOrNull() ?: return@launch

                _uiState.update { uiState ->
                    if (uiState.lastUpdateMillis == null) uiState.copy(lastUpdateMillis = lastUpdate) else uiState
                }
            }
        }

        /**
         * Полоска «цены не обновляются» показывается не на первой же секунде без
         * связи: разрыв на пару секунд чинится сам, и полоска на нём только
         * мигала бы.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        private fun observeStaleStream() {
            viewModelScope.launch {
                _uiState
                    .map { it.streamStatus }
                    .distinctUntilChanged()
                    .flatMapLatest { status ->
                        if (status == StreamStatus.LIVE) {
                            flowOf(false)
                        } else {
                            flow {
                                delay(PairsConstants.MainScreen.STALE_NOTICE_DELAY_MS.milliseconds)
                                emit(true)
                            }
                        }
                    }.distinctUntilChanged()
                    .collect { stale -> _uiState.update { it.copy(isStale = stale) } }
            }
        }

        /** Живой ли поток — то, по чему пользователь понимает, верить ли числам. */
        private fun observeConnectionState() {
            viewModelScope.launch {
                observeConnectionStateUseCase()
                    .map(StreamStatus::of)
                    .distinctUntilChanged()
                    .collect { status -> _uiState.update { it.copy(streamStatus = status) } }
            }
        }

        /**
         * После реконнекта видимые строки дотягиваются через REST. Сокет пропущенного
         * не досылает, и строка малоликвидной пары показывала бы цену часовой
         * давности, пока по ней не придёт следующее событие. Ошибку не показываем:
         * это фоновая догонка, а следующее событие сокета строку всё равно обновит.
         */
        private fun observeReconnects() {
            viewModelScope.launch {
                observeStreamReconnectsUseCase().collect {
                    refreshBestPricesUseCase(subscribedTickers.toSet()).onSuccess { updated ->
                        if (updated > 0) markUpdated()
                    }
                }
            }
        }

        // the flush job lives only while ticks keep coming: one interval with no
        // new updates and it exits, the next tick schedules it again
        private fun schedulePriceFlush() {
            synchronized(pendingPricesLock) {
                if (isFlushScheduled) return
                isFlushScheduled = true
            }

            viewModelScope.launch {
                while (true) {
                    delay(PairsConstants.MainScreen.PRICE_FLUSH_INTERVAL_MS.milliseconds)

                    val batch =
                        synchronized(pendingPricesLock) {
                            // клеим флаг к наблюдению пустоты: тик, добавленный
                            // до этого блока, попадёт в batch; добавленный после —
                            // увидит isFlushScheduled == false и запустит новый джоб
                            if (pendingPriceUpdates.isEmpty()) {
                                isFlushScheduled = false
                                return@launch
                            }
                            pendingPriceUpdates.values.toList().also { pendingPriceUpdates.clear() }
                        }

                    applyBestPriceChangesUseCase(batch)
                        .onSuccess { markUpdated() }
                        .onFailure { exception ->
                            _uiState.update { it.copy(error = exception.asAppError()) }
                        }
                }
            }
        }

        fun syncFavouriteSymbols() {
            viewModelScope.launch {
                syncFavouriteSymbolsUseCase().onFailure { exception ->
                    _uiState.update { it.copy(error = exception.asAppError()) }
                }
            }
        }

        /**
         * Вход необязателен, поэтому избранное — единственное, что его требует:
         * после выхода фильтр гаснет сам, иначе каталог остался бы пустым списком
         * без объяснения.
         */
        private fun observeAuthState() {
            viewModelScope.launch {
                observeAuthStateUseCase()
                    .map { user -> user != null }
                    .distinctUntilChanged()
                    .collect { signedIn ->
                        if (!signedIn) {
                            _uiState.update { uiState -> uiState.copy(onlyFavourite = false) }
                        }
                    }
            }
        }

        private fun observeFavouriteSymbols() {
            viewModelScope.launch {
                observeFavouriteSymbolsUseCase().collect { favourites ->
                    _uiState.update { it.copy(favouriteSymbolIds = favourites) }
                }
            }
        }

        override fun onCleared() {
            streamDisconnectUseCase()
        }

        private data class PairsFilter(
            val query: String,
            val onlyFavourite: Boolean,
            val favouriteSymbolIds: Set<Long>,
            val direction: CatalogDirection,
            val sorting: CatalogSorting,
        )
    }
