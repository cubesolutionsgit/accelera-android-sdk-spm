package ai.accelera.library.compose

import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

@Stable
class AcceleraBannerController {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var refreshAction: (() -> Unit)? = null
    private var detachAction: (() -> Unit)? = null

    fun refresh() {
        runOnMain { refreshAction?.invoke() }
    }

    fun detach() {
        runOnMain { detachAction?.invoke() }
    }

    internal fun bind(refresh: () -> Unit, detach: () -> Unit) {
        refreshAction = refresh
        detachAction = detach
    }

    internal fun unbind(refresh: () -> Unit, detach: () -> Unit) {
        if (refreshAction === refresh) refreshAction = null
        if (detachAction === detach) detachAction = null
    }

    private fun runOnMain(action: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            action()
        } else {
            mainHandler.post(action)
        }
    }
}

@Composable
fun rememberAcceleraBannerController(): AcceleraBannerController {
    return remember { AcceleraBannerController() }
}
