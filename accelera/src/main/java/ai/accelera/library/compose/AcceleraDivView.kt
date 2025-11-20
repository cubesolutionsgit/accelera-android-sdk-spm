package ai.accelera.library.compose

import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.yandex.div.core.view2.Div2View

/**
 * Composable for displaying DivKit content.
 *
 * @param jsonData JSON data for DivKit content
 * @param modifier Modifier for the composable
 */
@Composable
fun AcceleraDivView(
    jsonData: ByteArray,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val divView = remember(jsonData) {
        DivKitSetup.makeView(context, jsonData, lifecycleOwner)
    }

    LaunchedEffect(jsonData) {
        val divData = DivKitSetup.parseDivData(jsonData)
        if (divData != null) {
            val tag = com.yandex.div.DivDataTag("accelera_${System.currentTimeMillis()}")
            divView.setData(divData, tag)
        }
    }

    AndroidView(
        factory = { divView },
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
    )
}

