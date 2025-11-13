# Accelera SDK

Библиотека `Accelera` — это модульный SDK для интеграции динамического контента (баннеров и сторис) и push-уведомлений в ваше Android-приложение.

## 📦 Установка

### Через Maven

Добавьте репозиторий и зависимость в ваш `build.gradle.kts` (или `build.gradle`):

```kotlin
repositories {
    maven {
        url = uri("https://jitpack.io")
    }
    // или ваш Maven репозиторий
}

dependencies {
    implementation("com.github.cubesolutionsgit:accelera-android-sdk-spm:0.4.1")
}
```

### Добавление модуля в проект

Если вы используете библиотеку как локальный модуль, добавьте в ваш `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":accelera"))
}
```

Или соберите AAR файл и добавьте его в проект.

## ⚙️ Конфигурация

### Стандартная инициализация

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

### Кастомный API (без конфигурации)

Если вы хотите самостоятельно обрабатывать сетевые вызовы, достаточно настроить делегат:

```kotlin
Accelera.shared.configure(config = AcceleraConfig()) // пустой конфиг

Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {
    override val customAPI: AcceleraAPIProtocol? = MyCustomAPI()
})
```

```kotlin
class MyCustomAPI : AcceleraAPIProtocol {
    override fun loadBanner(
        data: ByteArray?,
        completion: (ByteArray?, NetworkError?) -> Unit
    ): Any? {
        // вызов вашего backend
        // completion(result, null) // успех
        // completion(null, NetworkError(...)) // ошибка
    }
    
    // ... остальные методы
}
```

> ☝️ Все методы взаимодействия с сервером — **POST**.

## 📐 Размещение контента

Контент отображается в контейнерах, которые вы добавляете на экран.

### Использование Jetpack Compose

```kotlin
import ai.accelera.library.compose.AcceleraBanner
import ai.accelera.library.compose.AcceleraStories

// Баннер
AcceleraBanner(
    data = mapOf(
        "type" to "banner",
        "category" to "main_screen"
    ),
    modifier = Modifier.fillMaxWidth()
)

// Сторис
AcceleraStories(
    data = mapOf(
        "type" to "stories"
    ),
    modifier = Modifier.fillMaxWidth()
)
```

### Использование View (ViewGroup)

#### Программно

```kotlin
import ai.accelera.library.banners.AcceleraBanners
import android.view.ViewGroup
import android.widget.LinearLayout

val storiesContainer = LinearLayout(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}

val bannerContainer = LinearLayout(context).apply {
    layoutParams = ViewGroup.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.WRAP_CONTENT
    )
}

parentView.addView(storiesContainer)
parentView.addView(bannerContainer)
```

#### Через XML Layout

1. Добавьте `ViewGroup` для баннеров и сторис в ваш layout:

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

2. Создайте ссылку в вашем Activity/Fragment:

```kotlin
val storiesContainer: ViewGroup = findViewById(R.id.stories_container)
val bannerContainer: ViewGroup = findViewById(R.id.banner_container)
```

Далее в обоих случаях нужно привязать элементы к библиотеке:

```kotlin
import ai.accelera.library.banners.AcceleraBanners
import ai.accelera.library.utils.toJsonBytes

AcceleraBanners.attachContentPlaceholder(
    container = storiesContainer,
    data = mapOf("type" to "stories").toJsonBytes()
)

AcceleraBanners.attachContentPlaceholder(
    container = bannerContainer,
    data = mapOf("type" to "banner").toJsonBytes()
)
```

### ℹ️ Параметры метода `attachContentPlaceholder`

| Параметр | Тип | Описание |
|----------|-----|----------|
| `container` | ViewGroup | Контейнер для отображения контента |
| `data` | ByteArray? | Конфигурация контента в формате JSON. Может содержать тип и параметры |

#### 🔸 Типы контента:

- `"stories"` — сторис (горизонтальная лента историй, при клике можно открыть на полный экран)
- `"banner"` — баннеры (статичный или карусель)

#### 🔹 Пример

```kotlin
AcceleraBanners.attachContentPlaceholder(
    container = bannerContainer,
    data = mapOf(
        "type" to "banner",
        "category" to "main_screen",
        "user_segment" to "premium"
    ).toJsonBytes()
)
```

Конкретный набор параметров зависит от вашей серверной конфигурации и бизнес-логики.

## ⚡️ Обработка действий

Если пользователь нажимает на баннер или сторис, или происходит другое действие — оно передаётся в делегат:

```kotlin
import ai.accelera.library.DefaultAcceleraDelegate

Accelera.shared.setDelegate(object : DefaultAcceleraDelegate() {
    override fun action(action: String) {
        Log.d("Accelera", "Действие: $action")
    }
})
```

## 👤 Информация о пользователе

```kotlin
Accelera.shared.setUserInfo(
    """
    {
        "clientId": "123",
        "email": "john@example.com",
        "theme": "dark"
    }
    """.trimIndent()
)
```

Вызывайте:
- после авторизации
- при смене профиля/темы
- при любом событии, которое может поменять вид контента

---

📄 Версия: `0.4.1`  
📆 Обновлено: октябрь 2025  
📫 Поддержка: [@cubesolutions](https://github.com/cubesolutionsgit)  
🔗 Репозиторий: [accelera-android-sdk-spm](https://github.com/cubesolutionsgit/accelera-android-sdk-spm)
