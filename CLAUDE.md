# CryptoCompare (CoinDiff)

Android-приложение: сравнение котировок одной криптопары на разных биржах.
Каталог тикеров и цены приходят с собственного бэкенда (REST + WebSocket),
история для свечного графика — со стороннего CryptoCompare min-api.

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

4. **График — один запрос на экран.**
   История берётся агрегатом (CCCAGG) без параметра биржи. Переключение биржи в
   `ExchangeSelector` **не** перезагружает график — это осознанно, чтобы не жечь лимиты API.
   Поэтому `ExchangeSelector` стоит **под** графиком, вплотную к `ExchangeInfoCard`,
   которой он и управляет, а под графиком подписано, что это агрегат. Пока селектор
   стоял сверху, на UX-тесте его читали как переключатель графика: близость обещала
   связь, которой нет. Не переносить обратно наверх.

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

7. **Один `NavHostController` на приложение.**
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
CRYPTOCOMPARE_API_KEY=<ключ min-api>
```

`BASE_URL` обязателен (сборка падает без него), `CRYPTOCOMPARE_API_KEY` — нет:
без ключа график просто покажет пустое состояние. Ключ получают на
https://developers.coindesk.com/ (старый кабинет — cryptocompare.com/cryptopian/api-keys).

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
- Не привязывать загрузку графика к выбору биржи.
- Не пробрасывать DTO (`*Dto`, `Get*Response`) выше `core:data`.
- Не обращаться из `feature:*` к репозиториям в обход use case'ов.
- Не вызывать `rememberNavController()` и не заводить `NavHost` внутри `feature:*`.
- Не подключать одну `feature:*` к другой ради общего компонента — выносить в `core:ui`.
- Не коммитить ключи и адреса бэкенда в репозиторий.
