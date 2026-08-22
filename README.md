# CryptoCompare (CoinDiff)

An Android app for comparing quotes of the same crypto pair across different
exchanges. The ticker catalog and live prices come from a custom backend
(REST + WebSocket); chart history is loaded from the third-party CryptoCompare
min-api.

- `applicationId`: `com.boomhaa.cryptocompare`
- code namespace: `com.cryptocompare.*`
- min SDK 26 / target SDK 36 / compile SDK 37.1 / JVM target 21

## Features

- **Pair catalog** with search, favourites and infinite paging — prices in the
  list update live over WebSocket.
- **Pair details**: spread across exchanges, an exchange selector, and a
  candlestick chart (M15 / H1 / H4 / D1 / W1).
  The last bar follows the live price: the screen subscribes to the pair over
  WebSocket and keeps moving close/high/low (or opens a new bar) as ticks come
  in. The same tick stream refreshes buy/sell prices on the exchange cards.
- **Pinch zoom** on the chart; the Y scale always fits the visible window.
- **Favourites** synced to Firestore for signed-in users.
- Email + Google sign-in (Firebase Auth).
- Dark theme and in-app language switching without activity recreation.

## Architecture

Unidirectional data flow:

```
UI → ViewModel → UseCase → Repository → (REST API | WebSocket | Room | Firebase)
```

- **ViewModels** expose a single immutable `StateFlow<UiState>`; each UI state
  lives in its own file.
- **Use cases** are one class per operation (`operator fun invoke(...)`).
- **Repositories** return `Result<T>` for one-shot operations and `Flow<T>`
  for streams, and expose domain models only. Errors never reach the UI as
  exceptions — repositories wrap them with `runCatching` and rethrow
  `CancellationException`.
- Features talk to `core:domain` only; they never touch repositories or each
  other.

### Module graph

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
 └── core:testing (tests only)
```

| Module | Responsibility |
|---|---|
| `core:model` | Domain POJOs grouped by domain (`model/ticker`, `model/chart`, …). Depends on nothing. |
| `core:domain` | Repository interfaces and use cases. No Room/Retrofit/UI knowledge. |
| `core:data` | Repository implementations, Room, mappers, Paging. The only layer that knows about both network and storage. DTOs do not leak above it. |
| `core:network` | Retrofit APIs, DTOs, the OkHttp WebSocket client. |
| `core:ui` | Theme, colors, typography, design tokens, shared components. |
| `core:helpers` | Extensions, price formatters, shared constants. |
| `core:testing` | Shared test fixtures (`MainDispatcherRule`). |

### Decisions worth knowing

1. **The catalog is Paging 3 backed by Room.** Pairs are aggregated in SQL;
   pages are fetched from the backend by a `RemoteMediator`. Nothing loads the
   whole catalog into memory.
2. **WebSocket ticks are batched.** Ticks arrive dozens of times per second;
   both the list and the detail screen accumulate the latest value and flush
   into UI state on a fixed interval instead of recomposing on every tick.
3. **The backend accepts at most 8 subscriptions per connection** (the 9th
   fails silently). The main screen syncs its visible rows against this limit;
   the detail screen takes over all subscription slots while open and restores
   the previous set on exit.
4. **One history request per screen.** The chart shows the CCCAGG aggregate;
   switching the selected exchange never reloads the chart.
5. **Room migrations ship as SQL assets**, destructive fallback is off.
6. **Price semantics:** backend `priceSell` = ask (you buy at this),
   `priceBuy` = bid (you sell at this). UI labels follow this inversion.

## Tech stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose (BOM 2026.06.01), Material 3, Navigation Compose |
| DI | Hilt 2.60.1 |
| Async | Coroutines 1.11 + Flow |
| Network | Retrofit 3 + Gson, OkHttp 5 (incl. WebSocket) |
| Storage | Room 2.8.4 (+ room-paging), DataStore |
| Paging | Paging 3.5 (`RemoteMediator` + `PagingSource`) |
| Charts | Vico 3.2.3 (`compose-m3`) |
| Images | Coil 3.5 (profile avatar only) |
| Auth | Firebase Auth (email + Google), Firestore for favourites |
| Background work | WorkManager |
| Style | ktlint (Gradle plugin 14.2.0), `.editorconfig` |

Kotlin 2.4.10, AGP 9.3.0, Gradle 9.6.1, KSP 2.3.10.

Note for AGP 9: the `org.jetbrains.kotlin.android` plugin is not applied in
modules — Kotlin support is built into AGP 9 and applying the plugin again
breaks configuration.

## Getting started

### Prerequisites

- JDK 21+
- Android SDK with platform 37.1
- A reachable instance of the quote backend (REST + WebSocket)

### Secrets

Secrets and endpoints are not stored in the repository — put them into
`~/.gradle/gradle.properties`:

```properties
DEBUG_BASE_URL=http://<host>:<port>/v1/
RELEASE_BASE_URL=http://<host>:<port>/v1/
DEBUG_WS_BASE_URL=ws://<host>
RELEASE_WS_BASE_URL=ws://<host>
CRYPTOCOMPARE_API_KEY=<min-api key>
```

`BASE_URL` is mandatory (the build fails without it).
`CRYPTOCOMPARE_API_KEY` is optional — without it the chart simply shows an
empty state. Get a key at https://developers.coindesk.com/.

### Build & run

```bash
./gradlew :app:assembleDebug   # build debug APK
./gradlew test                 # unit tests
./gradlew ktlintCheck          # style check
./gradlew ktlintFormat         # auto-format
```

## Testing

Unit tests cover ViewModels, use cases, repositories, mappers and helpers
(JUnit4 + MockK + Turbine + `kotlinx-coroutines-test`). Shared fixtures live in
`core:testing`. A test file sits in the same package as the class under test,
so no import of the tested class is needed.

Formatters and mappers are always covered by tests — they fail silently and
are easy to break.
