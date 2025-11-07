package ai.accelera.library.compose

import ai.accelera.library.Accelera
import ai.accelera.library.banners.AcceleraBanners
import ai.accelera.library.banners.CloseButton
import ai.accelera.library.banners.DivKitSetup
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.toJsonBytes
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

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
    val context = LocalContext.current
    var jsonData by remember { mutableStateOf<ByteArray?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(data) {
        isLoading = true
        val requestData = data?.toJsonBytes()
        
        Accelera.shared.getApi().loadBanner(requestData) { result, error ->
            if (error != null) {
                Accelera.shared.error("Failed to load content: ${error.message}")
                isLoading = false
            } else {
                val loadedData = result
                if (loadedData != null) {
                    jsonData = loadedData
                    
                    // Log view event (using extension property like iOS)
                    val metaValue = loadedData.meta
                    val eventPayload = mapOf(
                        "event" to "view",
                        "meta" to (metaValue ?: emptyMap<String, Any?>())
                    )
                    Accelera.shared.logEvent(eventPayload.toJsonBytes())
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
                            .padding(8.dp)
                    )
                }
            }
        }
    }
}

/**
 * Composable for displaying Accelera stories (horizontal scrollable).
 * Note: For fullscreen stories, use the FullscreenActivity.
 *
 * @param data Optional JSON data to send to the backend for loading content
 * @param modifier Modifier for the composable
 */
@Composable
fun AcceleraStories(
    data: Map<String, Any?>? = null,
    modifier: Modifier = Modifier
) {
    // Stories are typically displayed in a horizontal scrollable list
    // For now, we'll use the same banner implementation
    // Fullscreen stories are handled by FullscreenActivity
    AcceleraBanner(data = data, modifier = modifier)
}

