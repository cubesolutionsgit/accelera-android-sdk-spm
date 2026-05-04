package ai.accelera.library.compose

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.usecase.DefaultLoadBannerContentUseCase
import ai.accelera.library.banners.presentation.ui.CloseButton
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.toJsonBytes
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Composable for displaying Accelera banner content.
 *
 * @param data Optional JSON data to send to the backend for loading content
 * @param modifier Modifier for the composable
 */
@Composable
fun AcceleraBanner(
    data: Map<String, Any?>? = null,
    modifier: Modifier = Modifier
) {
    val requestData by remember(data) {
        derivedStateOf { data?.toJsonBytes() }
    }
    val requestKey by remember(requestData) {
        derivedStateOf { requestData?.decodeToString() ?: "__empty_request__" }
    }

    var jsonData by rememberSaveable(requestKey) { mutableStateOf<ByteArray?>(null) }
    var isLoading by rememberSaveable(requestKey) { mutableStateOf(jsonData == null) }
    val loadBannerContentUseCase = remember { DefaultLoadBannerContentUseCase() }

    LaunchedEffect(requestKey) {
        if (jsonData != null) return@LaunchedEffect

        isLoading = true
        loadBannerContentUseCase.load(requestData) { result, error ->
            if (error != null) {
                Accelera.shared.error("Failed to load content: ${error.message}")
                isLoading = false
            } else {
                val loadedData = result
                if (loadedData != null) {
                    jsonData = loadedData
                }
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        if (isLoading) {
            // Loading state - can be customized
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                contentAlignment = Alignment.Center
            ) {
                // Empty - can add loading indicator
            }
        } else if (jsonData != null) {
                AcceleraDivView(
                jsonData = jsonData!!,
                modifier = Modifier.fillMaxWidth()
            )

            // Close button if closable (using extension property like iOS)
            val currentJsonData = jsonData
            if (currentJsonData != null && currentJsonData.closable == true) {
                var showCloseButton by remember { mutableStateOf(true) }
                
                if (showCloseButton) {
                    AndroidView(
                        factory = { ctx ->
                            CloseButton(ctx).apply {
                                setOnClickListener {
                                    showCloseButton = false
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .statusBarsPadding()
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Composable for displaying Accelera stories (horizontal scrollable).
 * Stories are displayed as a horizontal ribbon, clicking on a story opens FullscreenActivity.
 * This uses the same mechanism as AcceleraBanner, just with different data parameters.
 *
 * @param data Optional JSON data to send to the backend for loading content (should contain "type": "stories")
 * @param modifier Modifier for the composable
 */
@Composable
fun AcceleraStories(
    data: Map<String, Any?>? = null,
    modifier: Modifier = Modifier
) {
    // Stories use the same implementation as banners - just different data
    // The click handling for fullscreen is done via div-action://fullscreen in AcceleraUrlHandler
    AcceleraBanner(data = data, modifier = modifier)
}

