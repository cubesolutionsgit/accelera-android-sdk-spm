package ai.accelera.library.core.logging

import ai.accelera.library.AcceleraDelegate
import android.os.Handler
import android.os.Looper

interface AcceleraLogger {
    fun setDelegate(delegate: AcceleraDelegate?)
    fun log(message: String)
    fun error(message: String)
}

class BufferedDelegateLogger(
    private val mainHandler: Handler? = runCatching {
        Looper.getMainLooper()?.let { Handler(it) }
    }.getOrNull()
) : AcceleraLogger {
    private val maxBufferedMessages = 100
    private val logBuffer = mutableListOf<String>()
    @Volatile
    private var delegate: AcceleraDelegate? = null

    override fun setDelegate(delegate: AcceleraDelegate?) {
        this.delegate = delegate
        if (delegate == null) {
            synchronized(logBuffer) { logBuffer.clear() }
            return
        }

        val buffered: List<String> = synchronized(logBuffer) {
            ArrayList(logBuffer).also { logBuffer.clear() }
        }
        if (buffered.isEmpty()) return
        if (mainHandler == null) {
            buffered.forEach(delegate::log)
        } else {
            mainHandler.post { buffered.forEach(delegate::log) }
        }
    }

    override fun log(message: String) {
        val currentDelegate = delegate
        if (currentDelegate == null) {
            buffer(message)
        } else {
            currentDelegate.log(message)
        }
    }

    override fun error(message: String) {
        val currentDelegate = delegate
        if (currentDelegate == null) {
            buffer(message)
        } else {
            currentDelegate.error(message)
        }
    }

    private fun buffer(message: String) {
        synchronized(logBuffer) {
            logBuffer.add(message)
            while (logBuffer.size > maxBufferedMessages) {
                logBuffer.removeAt(0)
            }
        }
    }
}
