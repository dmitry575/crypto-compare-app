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
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import com.cryptocompare.helpers.toUserMessage
import com.cryptocompare.pairs.R
import com.cryptocompare.pairs.ui.screens.mainScreen.components.EmptyState
import com.cryptocompare.pairs.ui.screens.mainScreen.components.ErrorState
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairRow
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairRowSkeleton
import com.cryptocompare.pairs.ui.screens.mainScreen.components.PairsSearchField
import com.cryptocompare.pairs.viewmodel.mainViewModel.MainViewModel
import com.cryptocompare.ui.components.AppSegmentedControl
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

    // Пусто по-настоящему, только когда загрузка дошла до конца: endOfPaginationReached
    // сбрасывается на время refresh, поэтому при переключении фильтра, пока список
    // ещё догружается, «ничего не найдено» не мелькает — вместо него скелетоны.
    val isEmpty =
        pairItems.itemCount == 0 &&
            pairItems.loadState.refresh is LoadState.NotLoading &&
            pairItems.loadState.append.endOfPaginationReached

    // первая загрузка провалилась и показывать нечего — не пустой экран, а ошибка с «Повторить»
    val firstLoadFailed = pagingError != null && pairItems.itemCount == 0

    val lazyList = rememberLazyListState()

    val snackbarHostState = remember { SnackbarHostState() }

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
        snapshotFlow { uiState.value.onlyFavourite }
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

            // фильтр, а не настройка: тумблер с подписью читался как переключатель
            // режима приложения, хотя выбирают, что показывать
            AppSegmentedControl(
                options = listOf(false, true),
                selected = uiState.value.onlyFavourite,
                onSelect = viewModel::onOnlyFavouriteChange,
                label = { onlyFavourite ->
                    stringResource(
                        if (onlyFavourite) R.string.pairs_filter_favorites else R.string.pairs_filter_all,
                    )
                },
            )

            when {
                firstLoadFailed -> {
                    ErrorState(
                        message = pagingError.error.toUserMessage(),
                        onRetry = pairItems::retry,
                    )
                }

                isEmpty -> {
                    EmptyState(
                        message =
                            if (uiState.value.onlyFavourite) {
                                stringResource(R.string.pairs_empty_favorites)
                            } else {
                                stringResource(R.string.pairs_empty_search, uiState.value.searchQuery)
                            },
                    )
                }

                pairItems.itemCount == 0 -> {
                    // ещё грузится или переключается фильтр — скелетоны, а не пустой экран
                    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                        val rowSpacing = Dimensions.Gap.sm
                        val rowHeight = Dimensions.Height.listItemSmall
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
                                PairRowSkeleton(rowHeight = rowHeight)
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
                                    rowHeight = Dimensions.Height.listItemSmall,
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
