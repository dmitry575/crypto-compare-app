package com.cryptocompare.pairs.ui.screens.mainScreen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.model.symbol.CatalogDirection
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.ui.screens.mainScreen.components.EmptyState
import com.cryptocompare.pairs.ui.screens.mainScreen.components.ErrorState
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairRow
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairRowSkeleton
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairsFilterRow
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairsListLegend
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairsSearchField
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairsSortSheet
import com.cryptocompare.pairs.util.PairsConstants
import com.cryptocompare.pairs.viewmodel.mainViewModel.MainViewModel
import com.cryptocompare.ui.theme.Dimensions
import com.cryptocompare.ui.theme.bgCard
import com.cryptocompare.ui.theme.bgPrimary
import com.cryptocompare.ui.theme.divider
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlin.math.ceil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onPairClick: (ticker: String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle()
    val pairItems = viewModel.pairs.collectAsLazyPagingItems()
    val focusManager = LocalFocusManager.current

    val pagingError =
        (pairItems.loadState.refresh as? LoadState.Error)
            ?: (pairItems.loadState.append as? LoadState.Error)

    // Фильтр, которому нечего показать по определению: звезда включена, а избранного
    // нет ни одного. Каталог тут не поможет, сколько его ни догружай.
    val filterMatchesNothing = uiState.value.onlyFavourite && uiState.value.favouriteTickers.isEmpty()

    // Локальная выборка исчерпана: по тому, что уже лежит в Room, под фильтр не
    // подходит ничего. Спрашиваем именно source, а не общий loadState: общий
    // endOfPaginationReached становится истинным только когда домедиатор выкачает
    // весь каталог, и до тех пор экран висел бы на скелетонах — пользователь всё это
    // время думает, что грузится, хотя показывать уже нечего.
    val localExhausted =
        pairItems.loadState.source.refresh is LoadState.NotLoading &&
            pairItems.loadState.source.append.endOfPaginationReached

    // Но первую порцию всё же дожидаемся: на чистой установке Room пуст, и без этого
    // «ничего не найдено» мелькало бы поверх ещё не приехавшего каталога. Догрузку
    // следующих страниц (mediator.append) не ждём — она идёт фоном и может длиться
    // сколько угодно, а ответ «под фильтр ничего не подходит» уже известен.
    val firstFillInProgress = pairItems.loadState.mediator?.refresh is LoadState.Loading

    val isEmpty =
        pairItems.itemCount == 0 &&
            (filterMatchesNothing || (localExhausted && !firstFillInProgress))

    // первая загрузка провалилась и показывать нечего — не пустой экран, а ошибка с «Повторить»
    val firstLoadFailed = pagingError != null && pairItems.itemCount == 0

    val lazyList = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

    // состояние шторки живёт в экране, а не во ViewModel: это не часть фильтра,
    // а то, открыт ли сейчас диалог
    var showSortSheet by remember { mutableStateOf(false) }

    LaunchedEffect(lazyList, viewModel, pairItems) {
        snapshotFlow {
            if (pairItems.itemCount == 0) {
                emptyList()
            } else {
                // отдаём всё, что реально на экране; лимит подписок применяет use case
                lazyList.layoutInfo.visibleItemsInfo
                    .mapNotNull { itemInfo ->
                        if (itemInfo.index < pairItems.itemCount) {
                            pairItems.peek(itemInfo.index)?.ticker
                        } else {
                            null
                        }
                    }
            }
        }.distinctUntilChanged()
            .collect(viewModel::onVisibleTickersChange)
    }

    // только на реальное переключение фильтра: LaunchedEffect отрабатывает и при
    // возврате на экран, из-за чего список прыгал в начало
    LaunchedEffect(lazyList) {
        snapshotFlow { Triple(uiState.value.onlyFavourite, uiState.value.direction, uiState.value.sorting) }
            .drop(1)
            .collect { lazyList.scrollToItem(0) }
    }

    LaunchedEffect(uiState.value.error) {
        uiState.value.error?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.onErrorShown()
        }
    }

    // при пустом списке ошибка занимает весь экран, и снекбар только дублировал бы её
    LaunchedEffect(pagingError) {
        if (pagingError != null && pairItems.itemCount > 0) {
            snackbarHostState.showSnackbar(pagingError.error.toUserMessage())
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.pairs_title),
                        style = MaterialTheme.typography.headlineMedium,
                    )
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.bgPrimary,
                    ),
                actions = {
                    IconButton(onClick = onProfileClick) {
                        Icon(
                            imageVector = Icons.Outlined.AccountCircle,
                            contentDescription = stringResource(R.string.pairs_open_profile),
                        )
                    }
                },
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.bgPrimary)
                    .padding(paddingValues)
                    .padding(horizontal = Dimensions.Padding.screenHorizontal)
                    .padding(vertical = Dimensions.Padding.screenVertical)
                    .pointerInput(Unit) {
                        detectTapGestures { focusManager.clearFocus() }
                    },
            verticalArrangement = Arrangement.spacedBy(Dimensions.Gap.md),
        ) {
            PairsSearchField(
                query = uiState.value.searchQuery,
                onQueryChange = viewModel::onSearchQueryChange,
            )

            // помещается ли объём в приглушённый ряд строки. Решается один раз на
            // экран, а не в каждой строке: ширина у всех строк одна, а замер в
            // строке означал бы субкомпозицию на каждый кадр прокрутки. Крупный
            // системный шрифт растит ряд, а ширину экрана — нет, отсюда fontScale
            val showVolumeInRow =
                LocalConfiguration.current.screenWidthDp.dp >=
                    PairsConstants.MainScreen.volumeInRowMinWidth * LocalDensity.current.fontScale

            // фильтр, а не настройка: тумблер с подписью читался как переключатель
            // режима приложения, хотя выбирают, что показывать
            PairsFilterRow(
                direction = uiState.value.direction,
                onDirectionChange = viewModel::onDirectionChange,
                onlyFavourite = uiState.value.onlyFavourite,
                onOnlyFavouriteChange = viewModel::onOnlyFavouriteChange,
                sorting = uiState.value.sorting,
                onSortClick = { showSortSheet = true },
            )

            if (showSortSheet) {
                PairsSortSheet(
                    sorting = uiState.value.sorting,
                    onSortSelected = { field ->
                        viewModel.onSortSelected(field)
                        showSortSheet = false
                    },
                    onDismiss = { showSortSheet = false },
                )
            }

            // подписи к числам строки — один раз над списком: в самой строке на
            // них нет места. Легенда стоит снаружи LazyColumn, потому что ниже
            // индексы его видимых элементов сопоставляются тикерам для подписок
            PairsListLegend(showVolume = showVolumeInRow)

            when {
                firstLoadFailed -> {
                    ErrorState(
                        message = pagingError.error.toUserMessage(),
                        onRetry = pairItems::retry,
                    )
                }

                isEmpty -> {
                    // порядок ветвей — по настоящей причине пустоты, а не по порядку
                    // фильтров: искал — виноват запрос; звезда включена, а избранного
                    // нет вовсе — виновата звезда, даже если сверху стоит «Растут»
                    EmptyState(
                        message =
                            when {
                                uiState.value.searchQuery.isNotEmpty() ->
                                    stringResource(R.string.pairs_empty_search, uiState.value.searchQuery)

                                uiState.value.onlyFavourite && uiState.value.favouriteTickers.isEmpty() ->
                                    stringResource(R.string.pairs_empty_favorites)

                                uiState.value.direction != CatalogDirection.ANY ->
                                    stringResource(R.string.pairs_empty_direction)

                                uiState.value.onlyFavourite ->
                                    stringResource(R.string.pairs_empty_favorites)

                                else ->
                                    stringResource(R.string.pairs_empty_search, uiState.value.searchQuery)
                            },
                    )
                }

                pairItems.itemCount == 0 -> {
                    // ещё грузится или переключается фильтр — скелетоны, а не пустой экран
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val rowSpacing = Dimensions.Gap.sm
                        val rowHeight = Dimensions.Height.listItemStats
                        val skeletonCount =
                            ceil((maxHeight + rowSpacing) / (rowHeight + rowSpacing))
                                .toInt()
                                .coerceAtLeast(1)

                        LazyColumn(
                            modifier =
                                Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.bgCard),
                        ) {
                            items(skeletonCount) {
                                PairRowSkeleton(minRowHeight = rowHeight)
                            }
                        }
                    }
                }

                else -> {
                    // один блок с разделителями вместо ряда отдельных карточек:
                    // рамка вокруг каждой строки дробила список на 40 прямоугольников
                    LazyColumn(
                        state = lazyList,
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.bgCard),
                    ) {
                        items(
                            count = pairItems.itemCount,
                            key = pairItems.itemKey { it.ticker },
                        ) { index ->
                            val pair = pairItems[index]
                            if (pair != null) {
                                if (index > 0) {
                                    HorizontalDivider(
                                        modifier =
                                            Modifier.padding(
                                                start = Dimensions.Padding.listItemHorizontal,
                                            ),
                                        color = MaterialTheme.colorScheme.divider,
                                    )
                                }
                                PairRow(
                                    pair = pair,
                                    minRowHeight = Dimensions.Height.listItemStats,
                                    showVolume = showVolumeInRow,
                                    isFavourite = pair.ticker in uiState.value.favouriteTickers,
                                    onFavouriteClick = { viewModel.onFavouriteClick(pair.ticker) },
                                    onClick = { onPairClick(pair.ticker) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
