package ai.accelera.library.compose

import ai.accelera.library.Accelera
import ai.accelera.library.banners.domain.usecase.DefaultLoadBannerContentUseCase
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.banners.presentation.ui.CloseButton
import ai.accelera.library.core.constants.AcceleraEvents
import ai.accelera.library.core.constants.AcceleraJsonKeys
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.mergeJSON
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.refreshPayloadJson
import ai.accelera.library.utils.toJsonBytes
import android.os.Handler
import android.os.Looper
import org.json.JSONObject
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
    modifier: Modifier = Modifier,
    controller: AcceleraBannerController? = null
) {
    val requestData by remember(data) {
        derivedStateOf { data?.toJsonBytes() }
    }
    val baseRequestKey by remember(requestData) {
        derivedStateOf { requestData?.decodeToString() ?: EMPTY_REQUEST_KEY }
    }

    var refreshIndex by remember { mutableStateOf(0) }
    var detachIndex by remember { mutableStateOf(0) }
    var isDetached by remember { mutableStateOf(false) }
    val requestKey = "$baseRequestKey:$refreshIndex:$detachIndex"

    var jsonData by rememberSaveable(requestKey) { mutableStateOf<ByteArray?>(null) }
    var isLoading by rememberSaveable(requestKey) { mutableStateOf(!isDetached && jsonData == null) }
    val loadBannerContentUseCase = remember { DefaultLoadBannerContentUseCase() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var activeRequestKey by remember { mutableStateOf<String?>(null) }

    DisposableEffect(controller) {
        if (controller == null) {
            onDispose { }
        } else {
            val refreshAction = {
                isDetached = false
                refreshIndex += 1
            }
            val detachAction = {
                jsonData?.let { currentJsonData ->
                    val meta = (currentJsonData.meta as? JSONObject) ?: JSONObject()
                    val payload = mapOf(
                        AcceleraJsonKeys.EVENT to AcceleraEvents.CLOSE,
                        AcceleraJsonKeys.META to meta
                    )
                    Accelera.shared.logEvent(payload.toJsonBytes())
                }
                jsonData = null
                isDetached = true
                isLoading = false
                detachIndex += 1
            }
            controller.bind(refreshAction, detachAction)
            onDispose { controller.unbind(refreshAction, detachAction) }
        }
    }

    val loadRequestData = remember(requestData, refreshIndex) {
        if (refreshIndex == 0) {
            requestData
        } else {
            mergeJSON(
                old = requestData?.let { String(it, Charsets.UTF_8) },
                new = refreshPayloadJson()
            )?.toByteArray(Charsets.UTF_8)
        }
    }

    LaunchedEffect(requestKey, isDetached) {
        if (isDetached || jsonData != null) {
            isLoading = false
            return@LaunchedEffect
        }

        isLoading = true
        activeRequestKey = requestKey
        loadBannerContentUseCase.load(loadRequestData) { result, error ->
            mainHandler.post {
                if (activeRequestKey != requestKey || isDetached) return@post
                if (error != null) {
                    Accelera.shared.error("Failed to load content: ${error.message}")
                    isLoading = false
                } else {
                    val loadedData = result
                    // Only show the banner when there is actually something to render.
                    if (loadedData != null && DivKitSetup.hasDisplayableContent(loadedData)) {
                        jsonData = loadedData
                    } else if (loadedData != null) {
                        Accelera.shared.log("No content to display")
                    }
                    isLoading = false
                }
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
                    .height(LOADING_PLACEHOLDER_HEIGHT),
                contentAlignment = Alignment.Center
            ) {
                // Empty - can add loading indicator
            }
        } else if (!isDetached && jsonData != null) {
            val currentJsonData = jsonData
            if (currentJsonData != null) {
                AcceleraDivView(
                    jsonData = currentJsonData,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Close button if closable (using extension property like iOS)
            if (currentJsonData != null && currentJsonData.closable == true) {
                AndroidView(
                    factory = { ctx ->
                        CloseButton(ctx).apply {
                            setOnClickListener {
                                val meta = (currentJsonData.meta as? JSONObject) ?: JSONObject()
                                val payload = mapOf(
                                    AcceleraJsonKeys.EVENT to AcceleraEvents.CLOSE,
                                    AcceleraJsonKeys.META to meta
                                )
                                Accelera.shared.logEvent(payload.toJsonBytes())
                                jsonData = null
                                isDetached = true
                                isLoading = false
                                detachIndex += 1
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(CLOSE_BUTTON_PADDING)
                )
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
    modifier: Modifier = Modifier,
    controller: AcceleraBannerController? = null
) {
    // Stories use the same implementation as banners - just different data
    // The click handling for fullscreen is done via div-action://fullscreen in AcceleraUrlHandler
    AcceleraBanner(data = data, modifier = modifier, controller = controller)
}

/** Sentinel request key used when no request payload is provided. */
private const val EMPTY_REQUEST_KEY = "__empty_request__"

/** Height reserved for the banner while content is loading. */
private val LOADING_PLACEHOLDER_HEIGHT = 100.dp

/** Inset of the overlay close button from the banner's top-end corner. */
private val CLOSE_BUTTON_PADDING = 8.dp

