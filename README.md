# Accelera SDK

Библиотека `Accelera` — модульный SDK для интеграции динамического контента (баннеров и сторис) в Android-приложение на основе [Yandex DivKit](https://divkit.tech/).

## Содержание

- [Установка](#-установка)
- [Инициализация](#-инициализация)
- [Делегат](#-делегат)
- [Информация о пользователе](#-информация-о-пользователе)
- [Отображение контента](#-отображение-контента)
  - [Jetpack Compose](#jetpack-compose)
  - [View / XML](#view--xml)
- [Обработка действий и диплинков](#-обработка-действий-и-диплинков)
- [Сторис: поведение при диплинке](#-сторис-поведение-при-диплинке)
- [Кастомный API](#-кастомный-api)

---

## 📦 Установка

### JitPack

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        maven { url = uri("https://jitpack.io") }
    }
}

// build.gradle.kts (app)
dependencies {
    implementation("com.github.cubesolutionsgit:accelera-android-sdk-spm:0.4.9")
}
```

### Локальный модуль

```kotlin
// build.gradle.kts (app)
dependencies {
    implementation(project(":accelera"))
}
```

---

## ⚙️ Инициализация

Вызывайте `configure` как можно раньше — например, в `Application.onCreate()` или в `Activity.onCreate()` до `setContent`.

```kotlin
import ai.accelera.library.Accelera
import ai.accelera.library.AcceleraConfig

Accelera.shared.configure(
    config = AcceleraConfig(
        url = "https://your-api-endpoint.com",
        systemToken = "your-system-token"
    )
)
```

| Поле | Описание |
|------|----------|
| `url` | Базовый URL API. SDK добавляет `/api/v1/content` и `/api/v1/events` автоматически |
| `systemToken` | Токен системы, передаётся в заголовке `Authorization` |
| `userInfo` | Опциональный JSON с данными пользователя (см. [ниже](#-информация-о-пользователе)) |

---

## 🔔 Делегат

Делегат — главная точка получения событий из SDK. Устанавливайте сразу после `configure`.

```kotlin
import ai.accelera.library.DefaultAcceleraDelegate

Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {

    // Диплинки и внешние ссылки из баннеров/сторис
    override fun handleUrl(url: android.net.Uri) {
        navController.tryDeepLinkNavigate(url.toString(), context)
    }

    // Произвольные действия (actionName из div-action://<actionName>)
    override fun action(action: String) {
        Log.d("Accelera", "action: $action")
    }

    // Расширенная версия с параметрами и метаданными контента
    override fun action(actionName: String, params: Map<String, String>, meta: Any?) {
        Log.d("Accelera", "action=$actionName params=$params")
    }

    // Логирование (по умолчанию — Log.d)
    override fun log(message: String) { Log.d("Accelera", message) }
    override fun error(error: String) { Log.e("Accelera", error) }
})
```

### Методы делегата

| Метод | Когда вызывается |
|-------|-----------------|
| `handleUrl(url)` | Пользователь нажал на ссылку в баннере или сторис |
| `action(action)` | Произвольное действие (строка) |
| `action(actionName, params, meta)` | Действие с параметрами и метаданными |
| `shouldDismissStoriesOnLink(url)` | Нажатие на ссылку внутри полноэкранных сторис (см. [ниже](#-сторис-поведение-при-диплинке)) |
| `log(message)` / `error(error)` | Сообщения SDK для отладки |
| `customAPI` | Переопределение сетевого слоя (см. [ниже](#-кастомный-api)) |

---

## 👤 Информация о пользователе

Пользовательский контекст автоматически добавляется к каждому запросу на загрузку контента. Обновляйте при смене профиля или авторизации.

```kotlin
Accelera.shared.setUserInfo(
    """
    {
        "client_id": "user-123",
        "language": "ru",
        "segment": "premium"
    }
    """.trimIndent()
)
```

> Набор полей определяется серверной конфигурацией вашего проекта.

---

## 📐 Отображение контента

### Jetpack Compose

Оба компонента безопасно используются внутри `LazyColumn` — состояние загруженного контента сохраняется при прокрутке.

```kotlin
import ai.accelera.library.compose.AcceleraBanner
import ai.accelera.library.compose.AcceleraStories

LazyColumn {
    item(key = "stories") {
        AcceleraStories(
            data = mapOf(
                "type" to "stories",
                "slot" to "home_top"
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    item(key = "banner") {
        AcceleraBanner(
            data = mapOf(
                "type" to "banner",
                "slot" to "messages_top_banner"
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

> **Важно:** используйте стабильный `key` для каждого item в `LazyColumn` — это гарантирует корректное восстановление состояния при прокрутке.

#### Параметры компонентов

| Параметр | Тип | Описание |
|----------|-----|----------|
| `data` | `Map<String, Any?>?` | Параметры запроса (тип, слот, канал и др.) — зависят от серверной конфигурации |
| `modifier` | `Modifier` | Стандартный Compose-модификатор |

---

### View / XML

#### XML-разметка

```xml
<LinearLayout
    android:id="@+id/stories_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical" />

<LinearLayout
    android:id="@+id/banner_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical" />
```

#### Kotlin

```kotlin
import ai.accelera.library.banners.AcceleraBanners
import ai.accelera.library.utils.toJsonBytes

val storiesContainer: ViewGroup = findViewById(R.id.stories_container)
val bannerContainer: ViewGroup  = findViewById(R.id.banner_container)

AcceleraBanners.attachContentPlaceholder(
    container = storiesContainer,
    data = mapOf("type" to "stories", "slot" to "home_top").toJsonBytes()
)

AcceleraBanners.attachContentPlaceholder(
    container = bannerContainer,
    data = mapOf("type" to "banner", "slot" to "messages_top_banner").toJsonBytes()
)
```

#### Параметры `attachContentPlaceholder`

| Параметр | Тип | Описание |
|----------|-----|----------|
| `container` | `ViewGroup` | Контейнер для отображения контента |
| `data` | `ByteArray?` | Параметры запроса в формате JSON-байт |

---

## ⚡️ Обработка действий и диплинков

Все нажатия на интерактивные элементы баннеров и сторис транслируются через делегат.

```kotlin
Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {

    override fun handleUrl(url: android.net.Uri) {
        // Вызывается при div-action://link?url=...
        navController.tryDeepLinkNavigate(url.toString(), context)
    }

    override fun action(actionName: String, params: Map<String, String>, meta: Any?) {
        // Вызывается при div-action://<actionName>?param=value
        when (actionName) {
            "view"      -> trackImpression(meta)
            "click"     -> trackClick(params)
        }
    }
})
```

### Типы div-action

| URI | Поведение |
|-----|-----------|
| `div-action://fullscreen?id=<entryId>` | Открывает `FullscreenActivity` со сторис |
| `div-action://link?url=<url>` | Передаёт URL в `delegate.handleUrl()` |
| `div-action://close` | Закрывает текущий экран (только внутри `FullscreenActivity`) |
| `div-action://<custom>` | Передаётся в `delegate.action()` |

---

## 🔁 Сторис: поведение при диплинке

По умолчанию при нажатии на ссылку внутри полноэкранных сторис SDK автоматически закрывает `FullscreenActivity` — чтобы пользователь сразу увидел экран назначения навигации.

Если в вашем сценарии ссылка открывает оверлей (bottom sheet, in-app браузер), сторис можно оставить открытыми:

```kotlin
Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {

    override fun handleUrl(url: android.net.Uri) {
        navController.tryDeepLinkNavigate(url.toString(), context)
    }

    /**
     * true  — закрыть сторис после перехода (по умолчанию)
     * false — оставить сторис открытыми (например, при открытии bottom sheet)
     */
    override fun shouldDismissStoriesOnLink(url: android.net.Uri): Boolean {
        return url.host != "sheet" // пример: оставить открытыми для внутренних оверлеев
    }
})
```

### Порядок выполнения при `shouldDismissStoriesOnLink = true`

1. `handleUrl(url)` вызывается сразу — навигация в `MainActivity` выполняется.
2. `FullscreenActivity` запускает анимацию закрытия.
3. После завершения анимации пользователь видит целевой экран.

---

## 🔧 Кастомный API

Используйте, если хотите полностью заменить сетевой слой SDK (например, для тестирования или проксирования).

```kotlin
import ai.accelera.library.api.AcceleraAPIProtocol
import ai.accelera.library.networking.NetworkError

class MyCustomAPI : AcceleraAPIProtocol {
    override fun loadBanner(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        // вызов вашего backend
        completion(responseBytes, null)   // успех
        // completion(null, NetworkError.Server(500, "error")) // ошибка
    }

    override fun logEvent(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ) {
        completion(null, null)
    }
}

Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {
    override val customAPI: AcceleraAPIProtocol = MyCustomAPI()
})
```

> Все запросы SDK — **POST**. При наличии `customAPI` стандартный `AcceleraAPI` (OkHttp) не используется.

---

## Требования

- Android minSdk **21**
- Kotlin **1.9+**
- Jetpack Compose BOM **2024.01+** (для Compose-компонентов)
- Добавьте `FullscreenActivity` в `AndroidManifest.xml` вашего приложения:

```xml
<activity
    android:name="ai.accelera.library.banners.presentation.ui.FullscreenActivity"
    android:theme="@style/Theme.AppCompat.NoActionBar" />
```

---

📄 Версия: `0.4.9`
📆 Обновлено: март 2026
📫 Поддержка: [@cubesolutionsgit](https://github.com/cubesolutionsgit)
🔗 Репозиторий: [accelera-android-sdk-spm](https://github.com/cubesolutionsgit/accelera-android-sdk-spm)
