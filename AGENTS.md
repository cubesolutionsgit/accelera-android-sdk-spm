# AGENTS.md

This file provides guidance to Codex (Codex.ai/code) when working with code in this repository.

## Project Overview

**Accelera SDK** — an Android library (`accelera` module) for integrating dynamic content (banners and stories) into apps using [Yandex DivKit](https://divkit.tech/) for rendering. The `app` module is a minimal demo host.

Current version: `0.6.4`, distributed via JitPack (`com.github.cubesolutionsgit:accelera-android-sdk-spm`).

## Build Commands

```bash
# Build the library
./gradlew :accelera:assembleRelease

# Build debug AAR
./gradlew :accelera:assembleDebug

# Run unit tests
./gradlew :accelera:test

# Run a single test class
./gradlew :accelera:testDebugUnitTest --tests "ai.accelera.library.utils.AcceleraContentJsonTest"

# Build the demo app
./gradlew :app:assembleDebug

# Publish to local Maven
./gradlew :accelera:publishToMavenLocal
```

## Architecture

The library is structured around a singleton `Accelera.shared` entry point.

### Core Layer (`ai.accelera.library`)
- `Accelera` — singleton SDK entry point. Holds config, delegate, and API instance. All internal components call back through here for logging (`log`/`error`) and event tracking (`logEvent`).
- `AcceleraConfig` — serializable data class: `url`, `systemToken`, `userInfo`.
- `AcceleraDelegate` / `DefaultAcceleraDelegate` — callback interface for logging, URL handling, user actions, custom API override, and host font resolution via `fontProvider`.

### API Layer (`ai.accelera.library.api`)
- `AcceleraAPIProtocol` — interface with `loadBanner` and `logEvent`, both callback-based (`(ByteArray?, NetworkError?) -> Unit`).
- `AcceleraAPI` — default implementation; uses `WebClient` (OkHttp). Appends `/api/v1/content` and `/api/v1/events` to the configured base URL. Sends `Authorization` header with `systemToken`.
- `AcceleraAPIStub` — no-op fallback when no config/delegate is set.
- Custom API is injected via `AcceleraDelegate.customAPI`.

### Networking (`ai.accelera.library.networking`)
- `WebClient` — thin OkHttp wrapper; all requests are async callbacks. 30s timeouts.
- `NetworkError` — sealed class: `Timeout`, `Cancelled`, `NoConnection`, `Server(status, message)`, `InternalError(exception)`.

### Banners Module (`ai.accelera.library.banners`)
Entry point: `AcceleraBanners.attachContentPlaceholder(container: ViewGroup, data: ByteArray?)`.
It returns `AcceleraContentHandle` with `refresh()` and `detach()`. The singleton also exposes
container-keyed `refreshContentPlaceholder(container)` / `detachContentPlaceholder(container)`.
Attached content owns a shared DivKit variable scope. Fullscreen story cards opened from that
content receive the same `DivVariableController`, so `set_variable` changes propagate back to the
origin banner scope.

Flow:
1. Calls `Accelera.shared.getApi().loadBanner()` with merged user info.
2. Passes response JSON to `DivKitSetup.makeView()` + `parseDivData()`.
3. Renders a `Div2View` inside the container.
4. Fires a `"view"` event via `Accelera.shared.logEvent()`.
5. Optionally adds a `CloseButton` if the JSON contains `"closable": true`.

DivKit integration (`banners/infrastructure/divkit`):
- `DivKitSetup` — creates `Div2Context` + `DivConfiguration` with Glide image loader, Lottie extension, ExoPlayer factory, and `AcceleraUrlHandler`.
- `AcceleraUrlHandler` — handles `div-action://` URIs: `fullscreen` (launches `FullscreenActivity`), `link` (delegates to `AcceleraDelegate.handleUrl`), `refresh` (reloads attached content), `close` (finishes fullscreen/popup or removes attached content). `ignore` query key skips server event logging.

Stories fullscreen (`banners/presentation/ui/FullscreenActivity`):
- Launched with a payload **token** (`EXTRA_PAYLOAD_TOKEN`) + `entryId: String` extras; the JSON itself stays in `AcceleraPayloadRegistry` (raw `EXTRA_JSON_DATA` is legacy fallback only).
- Playback is orchestrated by `StoryPlaybackCoordinator` + `PlaybackStateMachine` (`banners/presentation/playback`), with `StoryProgressManager` (card timers), `PlayerLifecycleController` (video pause/resume), `EntryViewRepository` (entry containers), `StoryEntryPreloader`, `StoryEventLogger`.
- Gesture handling via `StoryGestureHandler`: tap left/right for card nav, swipe left/right for entry nav, swipe down to close, long-press to pause progress.

### DivKit Templates, Lottie & Host Fonts

- DivKit version is `32.61.4`; Lottie Android is `6.7.1` (see `gradle/libs.versions.toml`).
- `DivKitSetup.parseDivData(jsonData, templateSource = jsonData)` loads top-level `templates` into `DivParsingEnvironment` before parsing the card. Extracted story cards must pass the original payload as `templateSource`, including all preloading paths. Do not strip templates or expand them manually.
- Lottie JSON uses a `gif` element (`gif_url: "empty://"`) with the `lottie` extension; template-bound `$params`, absolute HTTPS `lottie_url`, and `repeat_count: -1` are supported. Errors go through the SDK logger.
- `AcceleraFontResolver` is shared by DivKit and Lottie within a content view. Fonts belong to the consuming app: automatically resolve TTF/OTF from its `assets/fonts`, or supply `AcceleraDelegate.fontProvider` for `res/font`, XML font families, and custom registries. Return null for unknown families to allow asset/system fallback.
- Install the font provider before rendering. Recreate views after changing it; font results are cached per view. Do not keep Activity references or font registries globally.
- Honor `font_family`, `font_weight`, and `font_weight_value`, including expression-based family names. Keep numeric weights 100–900 on Android 9+; AndroidX approximates on older supported versions. Variable fonts must be adapted to the requested weight even when it is 400: their default axis may differ.
- `AcceleraLottieExtensionHandler` installs fonts before the first drawable frame. Resolve Lottie `fFamily`, `fStyle`, and exact `fName`; preserve separate faces of one family. Never insert a family-wide font-map key that masks other weights. Preserve embedded glyph outlines and fonts from Lottie ZIP files.
- Missing fonts must fall back to a system font without crashing. Do not bundle demo fonts or customer JSON in the library AAR, and do not download arbitrary font URLs.
- Regression tests: `AcceleraFontResolverTest` covers asset TTF/OTF, provider priority, fallback, weights, exact faces, glyph preservation, and actual DivKit rendering; `LottieTemplateTest` covers shared templates and animation frame progression. Licensed test fonts belong only in `accelera/src/androidTest/assets/fonts`.
- Run on an unlocked Android device with its screen on:
  `./gradlew :accelera:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ai.accelera.library.banners.infrastructure.divkit.AcceleraFontResolverTest,ai.accelera.library.banners.infrastructure.divkit.LottieTemplateTest`
  The Lottie playback test loads an HTTPS animation and requires network access. On Windows use `.\gradlew.bat` and quote the entire `-P...` argument in PowerShell.
- Temporary customer fixtures, local demo screens, and API stubs must be removed before committing. Keep reusable regression tests and their font licenses.

### State & Lifecycle (in-memory, never in saved instance state)
Payloads are hundreds of KB, so **nothing content-sized may go into `onSaveInstanceState` or Intent extras** — that overflows the ~1MB Binder limit (`TransactionTooLargeException`). All cross-recreation state lives in process-memory registries under `banners/infrastructure/cache`:
- `BannerContentCache` — LRU (4MB, byte-sized) of loaded banner JSON, keyed by request key; `AcceleraBanner` seeds from it in `remember` so rotation/lazy-list scroll-back never re-fetches.
- `AcceleraPayloadRegistry` — hands JSON to `FullscreenActivity`/`PopupActivity` by UUID token in the Intent.
- `StoryPlaybackStateStore` — `StoryPlaybackSnapshot` (entry, card index, timer fraction, video positions) keyed by payload token; captured in `FullscreenActivity.onDestroy` when `!isFinishing`, passed to `StoryPlaybackCoordinator.open(entryId, restore)` on recreate.
- Registry entries (payload, snapshot, `AcceleraScopeRegistry` scope) are removed **only when `isFinishing`** — unconditional removal breaks rotation.
- Video resume: `TrackingPlayerFactory` in `DivKitSetup` observes `DivPlayer.Observer.onCurrentTimeChange` per player; `captureVideoPositions`/`applyVideoPositions` snapshot & seek by player creation order (pending seeks apply lazily as DivKit creates players during rebind).
- Process death: registries are empty → banner reloads, fullscreen/popup finish gracefully. This is intended.

### Compose Layer (`ai.accelera.library.compose`)
- `AcceleraBanner` — Composable; calls `loadBanner` in `LaunchedEffect`, renders `AcceleraDivView` (an `AndroidView` wrapper around `Div2View`).
- `AcceleraStories` — thin alias to `AcceleraBanner` (content type is differentiated by the `data` map, not a separate code path).

### Utils (`ai.accelera.library.utils`)
- `Extensions.kt` — `Map<String, Any?>.toJsonBytes()`, `mergeJSON()`, `ByteArray.meta`, `ByteArray.closable`.
- `ViewExtensions.kt` — `View.parentActivity` traversal helper.

## Key Design Decisions

- All API calls use `ByteArray` for request/response bodies (JSON bytes), not typed models. Parsing is done ad-hoc with `org.json.JSONObject`.
- User info is merged into every outgoing request payload via `Accelera.shared.addUserInfo(to:)`.
- The API instance is created lazily and reset on `configure()`. Custom API from delegate takes priority over `AcceleraAPI`.
- Log messages are buffered before a delegate is set and flushed when one is attached.
- Popup responses containing top-level `fullscreens` open `FullscreenActivity` with the first entry (sorted key), as if `div-action://fullscreen` fired; chosen by `PopupDestinationPolicy`.
- `FullscreenActivity` and `PopupActivity` must be declared in the consuming app's `AndroidManifest.xml` (the library manifest declares both under the `ai.accelera.library` namespace).
