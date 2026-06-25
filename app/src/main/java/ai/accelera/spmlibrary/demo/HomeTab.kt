package ai.accelera.spmlibrary.demo

import ai.accelera.library.Accelera
import ai.accelera.library.banners.AcceleraBanners
import ai.accelera.library.banners.AcceleraContentHandle
import ai.accelera.spmlibrary.BuildConfig
import ai.accelera.spmlibrary.ui.theme.SpmLibraryTheme
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun HomeTab(
    modifier: Modifier,
    configState: DemoConfigState,
    onConfigStateChange: (DemoConfigState) -> Unit,
    showError: (String) -> Unit
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var apiUrl by rememberSaveable { mutableStateOf(configState.appliedUrl.ifBlank { BuildConfig.ACCELERA_URL }) }
    var token by rememberSaveable { mutableStateOf(configState.appliedToken.ifBlank { BuildConfig.ACCELERA_TOKEN }) }
    var userInfo by rememberSaveable { mutableStateOf(defaultUserInfoJson) }
    var storiesData by rememberSaveable { mutableStateOf(defaultStoriesJson) }
    var bannerData by rememberSaveable { mutableStateOf(defaultBannerJson) }
    var popupData by rememberSaveable { mutableStateOf(defaultPopupJson) }
    var storiesAttached by rememberSaveable { mutableStateOf(false) }
    var bannerAttached by rememberSaveable { mutableStateOf(false) }

    var storiesContainer by remember { mutableStateOf<ViewGroup?>(null) }
    var bannerContainer by remember { mutableStateOf<ViewGroup?>(null) }
    var storiesHandle by remember { mutableStateOf<AcceleraContentHandle?>(null) }
    var bannerHandle by remember { mutableStateOf<AcceleraContentHandle?>(null) }
    val configDirty = apiUrl != configState.appliedUrl || token != configState.appliedToken
    val userInfoDirty = userInfo != configState.appliedUserInfo

    LaunchedEffect(storiesContainer, storiesAttached, storiesData, configState.isConfigured) {
        val container = storiesContainer
        if (storiesAttached && storiesHandle == null && container != null && configState.isConfigured) {
            jsonBytesOrNull(storiesData)?.let { data ->
                storiesHandle = AcceleraBanners.attachContentPlaceholder(container, data)
            }
        }
    }

    LaunchedEffect(bannerContainer, bannerAttached, bannerData, configState.isConfigured) {
        val container = bannerContainer
        if (bannerAttached && bannerHandle == null && container != null && configState.isConfigured) {
            jsonBytesOrNull(bannerData)?.let { data ->
                bannerHandle = AcceleraBanners.attachContentPlaceholder(container, data)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ConfigurationCard(
                apiUrl = apiUrl,
                token = token,
                userInfo = userInfo,
                configState = configState,
                configDirty = configDirty,
                userInfoDirty = userInfoDirty,
                onApiUrlChange = { apiUrl = it },
                onTokenChange = { token = it },
                onUserInfoChange = { userInfo = it },
                onConfigure = {
                    val trimmedUrl = apiUrl.trim()
                    val trimmedToken = token.trim()
                    if (trimmedUrl.isBlank() || trimmedToken.isBlank()) {
                        showError("API URL and system token are required before loading content")
                    } else {
                        configureAccelera(trimmedUrl, trimmedToken)
                        onConfigStateChange(
                            configState.copy(
                                appliedUrl = trimmedUrl,
                                appliedToken = trimmedToken,
                                appliedUserInfo = null
                            )
                        )
                        DemoEvents.log("Configured SDK")
                    }
                },
                onSetUserInfo = {
                    if (!configState.isConfigured) {
                        showError("Configure SDK before applying userInfo")
                    } else if (validateJson(userInfo, showError) != null) {
                        Accelera.shared.setUserInfo(userInfo)
                        onConfigStateChange(configState.copy(appliedUserInfo = userInfo))
                        DemoEvents.log("setUserInfo")
                    }
                }
            )
        }

        item {
            SectionCard(title = "UIView-style placeholders") {
                Text(
                    "Matches iOS attachContentPlaceholder(to:with:) with separate stories and banner containers.",
                    style = MaterialTheme.typography.bodySmall
                )
                JsonTextField("Stories data JSON", storiesData, { storiesData = it })
                ActionRow(
                    primary = "Attach" to {
                        if (!configState.isConfigured) {
                            showError("Configure SDK before loading stories")
                        } else {
                            val data = validateJson(storiesData, showError)
                            val container = storiesContainer
                            if (data == null) {
                                Unit
                            } else if (container == null) {
                                showError("Stories container is not ready")
                            } else {
                                storiesHandle = AcceleraBanners.attachContentPlaceholder(container, data)
                                storiesAttached = true
                                DemoEvents.log("attach stories placeholder")
                            }
                        }
                    },
                    secondary = "Refresh" to { storiesHandle?.refresh() ?: showError("No stories handle") },
                    tertiary = "Detach" to {
                        storiesAttached = false
                        storiesHandle?.detach() ?: showError("No stories handle")
                        storiesHandle = null
                    }
                )
                PlaceholderBox { container -> storiesContainer = container }

                HorizontalDivider()

                JsonTextField("Banner data JSON", bannerData, { bannerData = it })
                ActionRow(
                    primary = "Attach" to {
                        if (!configState.isConfigured) {
                            showError("Configure SDK before loading banner")
                        } else {
                            val data = validateJson(bannerData, showError)
                            val container = bannerContainer
                            if (data == null) {
                                Unit
                            } else if (container == null) {
                                showError("Banner container is not ready")
                            } else {
                                bannerHandle = AcceleraBanners.attachContentPlaceholder(container, data)
                                bannerAttached = true
                                DemoEvents.log("attach banner placeholder")
                            }
                        }
                    },
                    secondary = "Refresh" to { bannerHandle?.refresh() ?: showError("No banner handle") },
                    tertiary = "Detach" to {
                        bannerAttached = false
                        bannerHandle?.detach() ?: showError("No banner handle")
                        bannerHandle = null
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            bannerContainer?.let(Accelera.shared::refreshContentPlaceholder)
                                ?: showError("Banner container is not ready")
                        }
                    ) {
                        Text("Container refresh")
                    }
                    OutlinedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val container = bannerContainer
                            if (container == null) {
                                showError("Banner container is not ready")
                            } else {
                                bannerAttached = false
                                Accelera.shared.detachContentPlaceholder(container)
                                bannerHandle = null
                            }
                        }
                    ) {
                        Text("Detach by container")
                    }
                }
                PlaceholderBox { container -> bannerContainer = container }
            }
        }

        item {
            SectionCard(title = "Popup") {
                JsonTextField("Popup data JSON", popupData, { popupData = it })
                Button(
                    onClick = {
                        if (!configState.isConfigured) {
                            showError("Configure SDK before showing popup")
                            return@Button
                        }
                        val data = validateJson(popupData, showError) ?: return@Button
                        Accelera.shared.showPopup(context, data)
                        DemoEvents.log("showPopup")
                    }
                ) {
                    Text("Show popup")
                }
            }
        }
    }
}

@Composable
private fun ConfigurationCard(
    apiUrl: String,
    token: String,
    userInfo: String,
    configState: DemoConfigState,
    configDirty: Boolean,
    userInfoDirty: Boolean,
    onApiUrlChange: (String) -> Unit,
    onTokenChange: (String) -> Unit,
    onUserInfoChange: (String) -> Unit,
    onConfigure: () -> Unit,
    onSetUserInfo: () -> Unit
) {
    SectionCard(title = "Configuration") {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AssistChip(
                onClick = {},
                label = { Text(if (configState.isConfigured) "Configured" else "Not configured") }
            )
            AssistChip(
                onClick = {},
                label = { Text(if (configDirty) "Config draft changed" else "Config applied") }
            )
            AssistChip(
                onClick = {},
                label = { Text(if (configState.appliedUserInfo == null) "userInfo not set" else if (userInfoDirty) "userInfo draft changed" else "userInfo applied") }
            )
        }
        JsonTextField(
            label = "API URL",
            value = apiUrl,
            onValueChange = onApiUrlChange,
            singleLine = true
        )
        JsonTextField(
            label = "System token",
            value = token,
            onValueChange = onTokenChange,
            singleLine = true,
            secret = true
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                enabled = configDirty || !configState.isConfigured,
                onClick = onConfigure
            ) {
                Text(if (configDirty || !configState.isConfigured) "Apply config" else "Config applied")
            }
            OutlinedButton(
                enabled = configState.isConfigured && userInfoDirty,
                onClick = onSetUserInfo
            ) {
                Text(if (userInfoDirty) "Apply userInfo" else "userInfo applied")
            }
        }
        JsonTextField(
            label = "userInfo JSON",
            value = userInfo,
            onValueChange = onUserInfoChange
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ConfigurationCardPreview() {
    SpmLibraryTheme {
        ConfigurationCard(
            apiUrl = "https://api.example.com",
            token = "secret-token",
            userInfo = defaultUserInfoJson.trim(),
            configState = DemoConfigState(
                appliedUrl = "https://api.example.com",
                appliedToken = "secret-token"
            ),
            configDirty = false,
            userInfoDirty = true,
            onApiUrlChange = {},
            onTokenChange = {},
            onUserInfoChange = {},
            onConfigure = {},
            onSetUserInfo = {}
        )
    }
}
