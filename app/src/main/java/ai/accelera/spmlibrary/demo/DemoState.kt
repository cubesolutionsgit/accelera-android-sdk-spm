package ai.accelera.spmlibrary.demo

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Speed
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class DemoTab(
    val title: String,
    val icon: ImageVector
) {
    Home("Home", Icons.Filled.Home),
    Log("Log", Icons.AutoMirrored.Filled.Article),
    Compose("Compose", Icons.Filled.Code),
    Stress("Stress", Icons.Filled.Speed)
}

data class DemoLogEntry(
    val id: Long,
    val level: DemoLogLevel,
    val text: String
)

enum class DemoLogLevel {
    All,
    Info,
    Error,
    Action,
    Url
}

object DemoEvents {
    private const val TAG = "AcceleraDemo"

    private val mainHandler = Handler(Looper.getMainLooper())
    private val nextId = AtomicLong()

    val logEntries = mutableStateListOf<DemoLogEntry>()
    var snackbarSink: ((String) -> Unit)? = null

    fun log(message: String, level: DemoLogLevel = DemoLogLevel.Info) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
        val entry = DemoLogEntry(
            id = nextId.incrementAndGet(),
            level = level,
            text = "$timestamp  $message"
        )
        when (level) {
            DemoLogLevel.Error -> Log.e(TAG, message)
            else -> Log.i(TAG, message)
        }
        mainHandler.post {
            logEntries.add(entry)
            while (logEntries.size > 200) logEntries.removeAt(0)
        }
    }

    fun error(message: String) {
        log(message, DemoLogLevel.Error)
        mainHandler.post { snackbarSink?.invoke(message) }
    }

    fun clear() {
        mainHandler.post { logEntries.clear() }
    }
}

data class DemoConfigState(
    val appliedUrl: String,
    val appliedToken: String,
    val appliedUserInfo: String? = null
) {
    val isConfigured: Boolean
        get() = appliedUrl.isNotBlank() && appliedToken.isNotBlank()
}
