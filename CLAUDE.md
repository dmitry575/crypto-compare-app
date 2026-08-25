# CryptoCompare (CoinDiff)

Android-приложение: сравнение котировок одной криптопары на разных биржах.
Каталог тикеров, цены и история свечей приходят с собственного бэкенда
(REST + WebSocket). График строится по выбранной бирже: история берётся
постранично (`GET /v1/klines/{providerId}`, offset/limit) без локального кеша.

`applicationId` — `com.boomhaa.cryptocompare`, namespace кода — `com.cryptocompare.*`.

---

## Стек

| Слой | Технологии |
|---|---|
| UI | Jetpack Compose (BOM 2026.06.01), Material 3, Navigation Compose |
| DI | Hilt 2.60.1 |
| Асинхронность | Coroutines 1.11 + Flow |
| Сеть | Retrofit 3 + Gson, OkHttp 5 (в т.ч. WebSocket) |
| Локальные данные | Room 2.8.4 (+ room-paging) |
| Пагинация | Paging 3.5 (`RemoteMediator` + `PagingSource`) |
| Графики | vico 3.2.3 (`compose-m3`) |
| Картинки | Coil 3.5 (`coil-compose` + `coil-network-okhttp`) — только аватар профиля |
| Аутентификация | Firebase Auth (email + Google), Firestore для избранного |
| Фоновые задачи | WorkManager |
| Стиль | ktlint (gradle-плагин 14.2.0), `.editorconfig` |

Kotlin 2.4.10, AGP 9.3.0, Gradle 9.6.1, KSP 2.3.10, JVM target 21,
`minSdk 26`, `compileSdk 37.1`, `targetSdk 36`.

### Особенности сборки на AGP 9

- **Плагин `org.jetbrains.kotlin.android` не применяется в модулях** — начиная с AGP 9
  поддержка Kotlin встроена, и повторное применение плагина роняет конфигурацию.
  В `build.gradle.kts` модулей его быть не должно (в корневом каталоге алиас остался
  только как `apply false`).
- `compileSdk` задаётся новым DSL с минорной версией:
  ```kotlin
  compileSdk { version = release(37) { minorApiLevel = 1 } }
  ```
  Именно 37.1, потому что свежие androidx (`hilt-navigation-compose` и др.) требуют
  API 37+, а «чистого» android-37 в SDK нет.
- `targetSdk` намеренно остаётся 36: его подъём меняет рантайм-поведение и делается
  отдельно, а не заодно с обновлением зависимостей.

---

## Модули и направление зависимостей

```
app
 ├── feature:auth ───┐
 ├── feature:pairs ──┤
 ├── feature:profile ┤
 │                   ├──> core:domain ──> core:model
 │                   │         ▲
 │                   ├──> core:data ──> core:network ──> core:model
 │                   ├──> core:ui
 │                   └──> core:helpers
 └── core:testing (только для тестов)
```

Правила зависимостей:

- `core:model` — POJO-модели домена, не зависит ни от чего.
- `core:domain` — интерфейсы репозиториев и use case'ы. Не знает про Room, Retrofit, Android UI.
- `core:data` — реализации репозиториев, Room, мапперы, Paging. Единственный слой,
  который знает и про сеть, и про БД.
- `core:network` — Retrofit API, DTO, WebSocket-клиент. DTO **не** протекают выше `core:data`.
- `core:ui` — тема, цвета, типографика, `Dimensions`, а также компоненты, нужные больше
  чем одной фиче (`AppTextField`, `AppPrimaryButton`, `PasswordRequirements`).
- `core:helpers` — расширения и общие константы.
- `feature:*` — экраны и ViewModel. Обращаются только к `core:domain` (use case'ы), **не** к репозиториям напрямую.
  Фичи **не зависят друг от друга**: если компонент понадобился второй фиче, он переезжает
  в `core:ui` (вместе со своими строками и константами), а не импортируется из соседней фичи.

---

## Архитектура

Однонаправленный поток: `UI → ViewModel → UseCase → Repository → (API | Room)`.

- **ViewModel** отдаёт единственный `StateFlow<*UiState>`; UI-состояние — отдельный
  immutable data class в собственном файле (`MainUiState.kt`, `DetailUiState.kt`).
- **UseCase** — один класс = одна операция, `operator fun invoke(...)`.
- **Repository** возвращает `Result<T>` для разовых операций и `Flow<T>` для потоков.
  Наружу отдаёт только доменные модели.
- Ошибки не пробрасываются как исключения в UI: репозитории заворачивают в `runCatching`,
  `CancellationException` всегда перебрасывается дальше.

### Ключевые решения, которые легко сломать

1. **Каталог тикеров — Paging 3, а не список в памяти.**
   Источник истины — Room. `SymbolDao.pagingPairs()` агрегирует символы в пары прямо в SQL
   (`GROUP BY UPPER(ticker)`, min/max цены, поиск и фильтр избранного — тоже в запросе).
   `SymbolsRemoteMediator` подкачивает страницы каталога с бэкенда; позиция подкачки лежит
   в таблице `catalog_remote_key`. Не загружать весь каталог в память.

2. **Цены из WebSocket пишутся батчами.**
   Тики копятся в `Map<symbolId, TickerPrice>` и раз в `PRICE_FLUSH_INTERVAL_MS` уходят
   в БД одним заходом; Room сам инвалидирует видимые страницы. Обновлять UI на каждый тик нельзя —
   это давало непрерывный GC и пропуск кадров.

3. **Бэкенд принимает максимум 8 подписок на соединение.**
   Девятая возвращает `Subscribe failed` (errorCode 2) — причём **без указания тикера**,
   сопоставить можно только по порядку ответов. Лимит живёт в
   `WebSocketConstants.MAX_SUBSCRIPTIONS` и применяется в `SyncVisibleTickersUseCase`.
   Экран отдаёт все видимые строки, use case берёт верхние: `min(видимые, MAX_SUBSCRIPTIONS)`.
   Высота строки списка фиксированная (`Dimensions.Height.listItemSmall`) и с этим лимитом
   не связана — раньше экран делил высоту на число тикеров, и на маленьком телефоне
   строки сплющивались.

4. **График — по выбранной бирже, история постранично.**
   Свечи берутся с нашего бэкенда по `providerId` (`GET /v1/klines/{providerId}`,
   offset/limit), поэтому график привязан к выбранной бирже и **перезагружается**
   при её смене в `ExchangeSelector`. Кеша на диске нет — истории слишком много;
   в памяти держим **скользящее окно** `ChartWindow` (≤ `WINDOW_MAX_CANDLES`) по
   ключу `providerId+timeframe`. История доступна целиком в обе стороны: у левого
   края `CandlestickChart` зовёт `loadOlderCandles()` (страница дописывается слева,
   свежий край выгружается), у правого — `loadNewerCandles()` (дописывается справа,
   старый край выгружается и снова становится догружаемым). Смещения бэкенда
   считаются в «возрасте» свечи: `newestSkip` — возраст самой свежей загруженной
   (0 = живой край), `oldestSkip` — offset следующей старой страницы. Позицию кадра
   при сдвиге окна сохраняет `Scroll.Relative.x(N)` (сдвиг в единицах X, не зависит
   от зума), склейка страниц — `mergeOlderCandles` с дедупом по времени. Пустая
   страница = у биржи глубже истории нет. Немаркетные периоды не растягивают ось:
   свечи рисуются по индексу, а не по времени.
   Пагинация графика **намеренно не на Paging 3** (в отличие от каталога): vico ест
   готовый `List`, а не `PagingData`, живой последний бар мутируется на каждый тик,
   кеша нет — интеграция Paging с ленивым списком тут не работает, а сложное (якорь
   скролла, дедуп, живой бар) всё равно решается руками. Поэтому — стейт в ViewModel.
   Раньше история бралась агрегатом (CCCAGG) со стороннего min-api и от биржи не
   зависела — отсюда прежнее «один запрос на экран» и селектор под графиком.

5. **Миграции Room лежат в assets, разрушающей миграции нет.**
   `assets/migrations/migration_<from>_<to>.sql`, подхватываются автоматически через
   `AssetMigrations.loadAll()`. Новая миграция = новый sql-файл + `version` в `@Database`
   + закоммиченная схема из `core/data/schemas/` (`exportSchema = true`).
   Сплиттер режет по `;`, поэтому триггеры и литералы с `;` пишутся кодом, а не в sql.
   `fallbackToDestructiveMigration` в `DatabaseModule` быть не должно: его аргумент —
   это `dropAllTables`, а не «включено/выключено», так что вызов с `false` разрушающую
   миграцию **включает** и молча стирает избранное. Подробности — `core/data/MIGRATIONS.md`.

6. **Семантика цен.** Бэкенд отдаёт `priceSell` = ask (по ней пользователь покупает),
   `priceBuy` = bid (по ней продаёт). В UI подписи соответственно инвертированы.

7. **Язык переключается рекомпозицией, а не `setApplicationLocales`.**
   Per-app locale (`AppCompatDelegate.setApplicationLocales`) пересоздаёт активность,
   и на время recreate экран пропадал и прыгал вверх-вниз, пока доезжали window insets.
   Плюс он требует `AppCompatActivity`: с `ComponentActivity` вызов проходит без ошибок
   и молча ничего не меняет. Поэтому язык живёт как тема — DataStore + `LanguageRepository`
   + `ProvideAppLanguage` в `core:ui`, который подменяет `LocalConfiguration`/`LocalContext`.
   `MainActivity` остаётся `ComponentActivity`.

   В `LocalContext` уходит **`ContextWrapper` над самой активностью** с подменённым
   `getResources()`, а не результат `createConfigurationContext()`. Тот возвращает
   `ContextImpl`, и всё, что ищет Activity обходом `baseContext` — `hiltViewModel()`,
   наш `findActivity()` в Google-входе, — падает с «Expected an activity context».
   Тему у обёртки не подменяем: у `createConfigurationContext` она дефолтная.

   `ProvideAppLanguage` вызывается всегда, в том числе для `AppLanguage.SYSTEM`:
   если убирать его из композиции по условию, поддерево пересоздаётся и теряет состояние.
   Новый язык = элемент `AppLanguage` + `values-<tag>` + строка-название.

8. **Один `NavHostController` на приложение.**
   `rememberNavController()` вызывается ровно один раз — в `AppNavigation`. Фичи не заводят
   свой `NavHost`, а отдают вложенный граф:
   ```kotlin
   fun NavGraphBuilder.pairsNavigation(navController: NavHostController, onProfileClick: () -> Unit)
   ```
   внутри которого `navigation(route = PairsDestination.ROUTE, startDestination = ...)`.
   `*Destination.ROUTE` — это маршрут графа фичи, `*Screens` — маршруты экранов внутри него.
   Переходы между фичами задаются лямбдами в `AppNavigation`: фичи друг о друге не знают.
   Вложенные `NavHost` дают каждой фиче отдельный back stack, из-за чего `popUpTo` по чужому
   маршруту молча не срабатывает.

---

## Конвенции кода

### Один тип — один файл

- **Каждый `data class` живёт в собственном файле**, имя файла = имя класса.
  Это касается и DTO: `GetHistoryResponse`, `HistoryDataDto`, `HistoryCandleDto`,
  `HistoryErrorDto` — четыре файла, а не один.
- Так же для `enum class`, `sealed`-иерархий и объектов-констант.
  Исключение — вложенные варианты `sealed`-иерархии: они остаются внутри родителя
  (`TickerStreamEvent`, `ConnectionState`).
- Исключение — дизайн-токены `core:ui` (`Dimensions`, `Type`): это единые наборы
  токенов, их не дробим.

### Константы

Никаких «магических» чисел и строк в логике и в разметке.

- **Один объект констант — один файл** в `util/`-папке своего модуля.
  Общие для нескольких модулей лежат в `core:helpers/util/`:
  `AppConstants.kt`, `WebSocketConstants.kt`, `CryptoCompareRepositoryConstants.kt`,
  `UseCaseConstants.kt`, `WorkerConstants.kt`, `FirestoreConstants.kt`,
  `PriceFormatConstants.kt`. Общего объекта-свалки `Constants` больше нет.
- Локальные для модуля → свой файл: `feature/pairs/.../util/PairsConstants.kt`,
  `core/data/.../util/DataConstants.kt`. Внутри можно группировать вложенными
  объектами по экрану/назначению (`PairsConstants.MainScreen`, `…Chart`).
- `private const val` внутри файла с логикой не заводим — константа переезжает в `*Constants.kt`.
- Ключи навигационных аргументов, имена ассетов, интервалы, лимиты — тоже константы.

### Compose

- **Одна публичная `@Composable` на файл.** Имя файла = имя функции
  (`PairRow.kt` → `fun PairRow`).
- Вспомогательные composable выносятся в отдельные файлы в папку `components/`
  рядом с экраном. Если такой helper нужен только своему модулю — помечаем `internal`.
- Единственное исключение: функция с `@Preview` может лежать рядом с тем, что она превьюит.
- Экран (`*Screen.kt`) собирает состояние и делегирует отрисовку компонентам, сам не
  содержит вложенных определений composable.
- Размеры берём из `core:ui` → `Dimensions` (`Spacing`, `Padding`, `Radius`, `Gap`, `Border`).
  Литералы `.dp` в UI-коде — только если размер действительно уникален, и тогда он
  выносится в `*Constants.kt` модуля.
- Цвета — только из `MaterialTheme.colorScheme` и расширений в `ThemeExtensions.kt`.

### Дизайн-система

1. **Акцент один.** `primary` и `secondary` в обеих схемах — один и тот же цвет.
   Раньше `secondary` был фиолетовым, и два соседних ряда чипов на детальном экране
   красились по-разному просто потому, что брали разные роли.
2. **Зелёный и красный заняты направлением цены.** `cryptoSuccess`/`cryptoError`
   и цвета графика не используются ни для чего другого. Большой разброс между
   биржами подсвечивается акцентом, а не зелёным: это не «подорожало».
3. **У каждого токена есть вариант под обе темы.** Токен без пары — ошибка:
   так `#00FF88` давал на белом контраст 1.5:1, а сетка графика была почти чёрной.
   Тема берётся из `LocalIsDarkTheme`, а не из `isSystemInDarkTheme()`.
4. **Числа — моноширинные и табличные** (`NumericType`). Пропорциональный шрифт
   не выравнивает разряды в колонке и заставляет цену дёргаться на каждом тике.
5. **Шкала шрифтов — 28/20/16/14/12**, три начертания. Новых размеров между ними
   не заводим; свои `TextStyle` — только если роли Material действительно не хватает.
6. **Эмодзи в интерфейсе нет.** Они рисуются шрифтом системы и на разных версиях
   Android выглядят по-разному. Иконки векторные, знак приложения — `Canvas`.

### Строки

Пользовательские строки — в `res/values/strings.xml` своего модуля, через `stringResource(...)`.
Хардкод текста в composable не допускается (и не смешиваем языки в одном экране).

### Именование и структура

- Пакеты — строчными буквами, без camelCase.
- `feature/<name>/ui/screens/<screen>/` — экран, рядом `components/`.
- `feature/<name>/viewmodel/<screen>/` — `*ViewModel.kt` и `*UiState.kt`.
- Мапперы — в `core:data/mapper`, по одному файлу на сущность (`SymbolsMapper.kt`, `CandleMapper.kt`).

#### Модели (`core:model`)

Модели разложены по подпакетам предметной области, а не свалены в корень:

```
model/auth/      AuthUser
model/provider/  Provider, ProviderStatus, ProviderDetail
model/symbol/    Symbol, PairUiItem, PairAggregateRow
model/ticker/    TickerPrice, TickerDetail, TickerStreamEvent, TickerConnectionState
model/chart/     Candle, ChartTimeframe, HistoryResolution, HistoryRequestSpec
model/settings/  ThemePreference
```

Собственной папки `model/` у других модулей нет: доменные модели живут только здесь,
даже если пользуется ими один слой (так `HistoryResolution` переехал из `core:data`).

Новая модель кладётся в подходящий подпакет; если её тема новая — заводим новый
подпакет, а не корень `model/`.

### Тесты

- Unit-тесты на ViewModel, use case'ы, репозитории, мапперы и хелперы (JUnit4 + MockK +
  Turbine + `kotlinx-coroutines-test`). Общее — в `core:testing` (`MainDispatcherRule`).
- **Тест лежит в том же пакете, что и тестируемый класс**:
  `usecase/pairs/LoadPairsUseCase.kt` → `test/.../usecase/pairs/LoadPairsUseCaseTest.kt`.
  Импорт самого класса тогда не нужен — ktlint считает его лишним.
- Один тестовый класс на предмет теста, если в нём есть логика. Тонкие делегации
  (`operator fun invoke() = repository.foo()`) группируются по репозиторию в один класс
  (`AuthUseCasesTest`, `FavouriteTickerUseCasesTest`), а имя теста начинается с имени
  use case'а — чтобы поиск по классу его находил.
- Для новых форматтеров и мапперов тест обязателен: они молча портят UI.

---

## Сборка и запуск

Секреты и адреса не хранятся в репозитории — только в `~/.gradle/gradle.properties`:

```properties
DEBUG_BASE_URL=http://<host>:<port>/v1/
RELEASE_BASE_URL=http://<host>:<port>/v1/
DEBUG_WS_BASE_URL=ws://<host>
RELEASE_WS_BASE_URL=ws://<host>
```

`BASE_URL` обязателен (сборка падает без него). Стороннего ключа больше нет —
история свечей идёт с нашего же бэкенда по `BASE_URL`.

Команды:

```bash
./gradlew :app:assembleDebug
./gradlew test              # unit-тесты
./gradlew ktlintCheck       # стиль
./gradlew ktlintFormat      # автоформат
```

---

## Чего не делать

- Не тянуть весь каталог символов в память и не подписываться на всю таблицу `symbols`.
- Не обновлять UI-состояние на каждый тик WebSocket.
- Не кешировать историю свечей в Room — её слишком много; страницы живут только в памяти ViewModel.
- Не пробрасывать DTO (`*Dto`, `Get*Response`) выше `core:data`.
- Не обращаться из `feature:*` к репозиториям в обход use case'ов.
- Не вызывать `rememberNavController()` и не заводить `NavHost` внутри `feature:*`.
- Не подключать одну `feature:*` к другой ради общего компонента — выносить в `core:ui`.
- Не коммитить ключи и адреса бэкенда в репозиторий.
