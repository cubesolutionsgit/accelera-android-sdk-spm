# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Accelera SDK** — an Android library (`accelera` module) for integrating dynamic content (banners and stories) into apps using [Yandex DivKit](https://divkit.tech/) for rendering. The `app` module is a minimal demo host.

Current version: `0.4.4`, distributed via JitPack (`com.github.cubesolutionsgit:accelera-android-sdk-spm`).

## Build Commands

```bash
# Build the library
./gradlew :accelera:assembleRelease

# Build debug AAR
./gradlew :accelera:assembleDebug

# Run unit tests
./gradlew :accelera:test

# Run a single test class
./gradlew :accelera:test --tests "ai.accelera.library.ExampleUnitTest"

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
- `AcceleraDelegate` / `DefaultAcceleraDelegate` — callback interface for logging, URL handling, user actions, and custom API override.

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

Flow:
1. Calls `Accelera.shared.getApi().loadBanner()` with merged user info.
2. Passes response JSON to `DivKitSetup.makeView()` + `parseDivData()`.
3. Renders a `Div2View` inside the container.
4. Fires a `"view"` event via `Accelera.shared.logEvent()`.
5. Optionally adds a `CloseButton` if the JSON contains `"closable": true`.

DivKit integration (`banners/infrastructure/divkit`):
- `DivKitSetup` — creates `Div2Context` + `DivConfiguration` with Glide image loader, Lottie extension, ExoPlayer factory, and `AcceleraUrlHandler`.
- `AcceleraUrlHandler` — handles `div-action://` URIs: `fullscreen` (launches `FullscreenActivity`), `link` (delegates to `AcceleraDelegate.handleUrl`), `close` (finishes activity).

Stories fullscreen (`banners/presentation/ui/FullscreenActivity`):
- Launched with `jsonData: ByteArray` + `entryId: String` extras.
- Follows clean architecture with dedicated use-case/manager classes: `StoryNavigationUseCase`, `StoryProgressManager`, `StoryContainerManager`, `StoryTransitionManager`, `StoryEntryLoader`, `StoryEntryPreloader`, `StoryEventLogger`, `StoryCardDisplayUseCase`.
- Gesture handling via `StoryGestureHandler`: tap left/right for card nav, swipe left/right for entry nav, swipe down to close, long-press to pause progress.

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
- `FullscreenActivity` must be declared in the consuming app's `AndroidManifest.xml` (the library's manifest declares it under the `ai.accelera.library` namespace).
