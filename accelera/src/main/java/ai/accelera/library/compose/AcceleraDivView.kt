package ai.accelera.library.compose

import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.div.DivDataTag

/**
 * Composable for displaying DivKit content.
 *
 * Design notes:
 * - Parsing is done synchronously in [remember] (not via produceState / LaunchedEffect).
 *   By the time this composable is shown, [jsonData] is already in RAM (loaded from the network
 *   in AcceleraBanner). Parsing takes ~1 ms and keeping it synchronous guarantees that [setData]
 *   is called before the first layout pass, which fixes two bugs:
 *     1. Banner disappears on LazyColumn scroll-back (view blank until async work completes).
 *     2. First-tap deeplink/action silently dropped (DivKit has no actions before setData).
 * - [setData] is only in [AndroidView.update], not in factory. update is called synchronously
 *   in the same composition frame as factory, so there is no blank-frame gap.
 * - [key(lifecycleOwner)] forces view recreation if the LifecycleOwner changes (e.g. fragment
 *   recreation inside a navigation graph).
 * - [onReset]/[onRelease] replace DisposableEffect for ExoPlayer cleanup; these hooks are the
 *   correct AndroidView-level mechanism and fire at the right moments in lazy layouts.
 *
 * @param jsonData JSON data for DivKit content
 * @param modifier Modifier for the composable
 */
@Composable
fun AcceleraDivView(
    jsonData: ByteArray,
    modifier: Modifier = Modifier
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val divData = remember(jsonData) { DivKitSetup.parseDivData(jsonData) }
    val divTag = remember { DivDataTag("accelera_banner") }

    key(lifecycleOwner) {
        AndroidView(
            factory = { ctx ->
                DivKitSetup.makeView(ctx, jsonData, lifecycleOwner)
            },
            update = { view ->
                divData?.let { view.setData(it, divTag) }
            },
            onReset = { view ->
                // View is about to be reused for a different item in a lazy layout.
                DivKitSetup.releaseVideoPlayers(view)
            },
            onRelease = { view ->
                // View is permanently removed from composition.
                DivKitSetup.releaseVideoPlayers(view)
            },
            modifier = modifier
                .fillMaxWidth()
                .wrapContentHeight()
        )
    }
}
