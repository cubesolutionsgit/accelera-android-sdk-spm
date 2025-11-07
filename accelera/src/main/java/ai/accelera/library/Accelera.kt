package ai.accelera.library

import ai.accelera.library.api.AcceleraAPI
import ai.accelera.library.api.AcceleraAPIProtocol
import ai.accelera.library.api.AcceleraAPIStub
import ai.accelera.library.utils.mergeJSON
import ai.accelera.library.utils.toJsonBytes
import kotlinx.serialization.json.Json
import org.json.JSONObject

/**
 * Main library class for Accelera SDK.
 * Provides singleton access via [shared] instance.
 */
class Accelera private constructor() {
    
    companion object {
        /**
         * Singleton instance to communicate with the library.
         */
        @JvmStatic
        val shared: Accelera = Accelera()
    }

    @Volatile
    private var config: AcceleraConfig? = null

    private val logBuffer = mutableListOf<String>()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @Volatile
    private var delegate: AcceleraDelegate? = null
    
    @Volatile
    private var api: AcceleraAPIProtocol? = null

    /**
     * Gets the current delegate.
     * @return Current delegate or null if not set
     */
    fun getDelegate(): AcceleraDelegate? = delegate

    /**
     * Sets delegate for library events.
     * This method pushes buffered logs if delegate was set later.
     * 
     * @param newDelegate The delegate to set, can be null
     */
    fun setDelegate(newDelegate: AcceleraDelegate?) {
        delegate = newDelegate
        pushBufferedLogs(newDelegate)
    }

    private fun pushBufferedLogs(currentDelegate: AcceleraDelegate?) {
        if (currentDelegate != null) {
            val messages: List<String>
            synchronized(logBuffer) {
                messages = if (logBuffer.isNotEmpty()) {
                    ArrayList(logBuffer).also { logBuffer.clear() }
                } else {
                    emptyList()
                }
            }
            if (messages.isNotEmpty()) {
                mainHandler.post {
                    messages.forEach { msg ->
                        currentDelegate.log(msg)
                    }
                }
            }
        } else {
            synchronized(logBuffer) {
                logBuffer.clear()
            }
        }
    }

    /**
     * Configures the library with the provided configuration.
     * 
     * @param config Configuration object with API URL and token
     */
    fun configure(config: AcceleraConfig) {
        synchronized(this) {
            this.config = config
            this.api = null // Reset API to force recreation
        }
        
        try {
            val json = Json.encodeToString(config)
            log("Accelera configured with config:\n$json")
        } catch (e: Exception) {
            error("Accelera configured (failed to encode config to JSON)")
        }

        configureBannersModule()
    }

    /**
     * Sets user information.
     * 
     * @param userInfo JSON string with user information
     */
    fun setUserInfo(userInfo: String?) {
        val currentConfig = synchronized(this) { config } ?: run {
            error("Can't set userInfo — Accelera is not configured!")
            return
        }

        val mergedUserInfo = mergeJSON(currentConfig.userInfo, userInfo) ?: userInfo
        
        synchronized(this) {
            this.config = currentConfig.copy(userInfo = mergedUserInfo)
        }

        log("User info set to: ${this.config?.userInfo ?: "nil"}")
    }

    /**
     * Logs an event for user activity.
     * 
     * @param event Event data as JSON bytes
     */
    fun logEvent(event: ByteArray) {
        try {
            val jsonString = String(event, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)
            val eventName = jsonObject.optString("event")
            if (eventName.isNotEmpty()) {
                delegate?.action(eventName)
            }
            log("Logging event: $jsonString")
        } catch (e: Exception) {
            log("Logging event: <invalid UTF-8 data>")
        }

        val dataWithUserInfo = addUserInfo(to = event)
        getApi().logEvent(dataWithUserInfo) { result, error ->
            if (error != null) {
                this@Accelera.error("Event error ${error.message}")
            } else {
                this@Accelera.log("Event sent successfully")
            }
        }
    }

    internal fun log(message: Any) {
        val msg = "[Accelera] $message"
        synchronized(logBuffer) {
            logBuffer.add(msg)
        }
        delegate?.log(msg)
    }

    internal fun error(error: Any) {
        val msg = "[Accelera] Error: $error"
        synchronized(logBuffer) {
            logBuffer.add(msg)
        }
        delegate?.error(msg)
    }

    internal fun handleUrl(url: android.net.Uri) {
        log("Handling URL: $url")
        delegate?.handleUrl(url)
    }

    internal fun getApi(): AcceleraAPIProtocol {
        return api ?: synchronized(this) {
            api ?: createApi().also { api = it }
        }
    }
    
    internal fun addUserInfo(to: ByteArray?): ByteArray? {
        val payload = mutableMapOf<String, Any?>()

        if (to != null) {
            try {
                val jsonString = String(to, Charsets.UTF_8)
                val dict = JSONObject(jsonString)
                val keys = dict.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    payload[key] = dict.get(key)
                }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        val userInfo = config?.userInfo
        if (userInfo != null) {
            try {
                val infoData = JSONObject(userInfo)
                payload["userInfo"] = infoData
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }

        return payload.toJsonBytes()
    }

    private fun createApi(): AcceleraAPIProtocol {
        val customAPI = delegate?.customAPI
        if (customAPI != null) {
            return customAPI
        }

        val currentConfig = config
        if (currentConfig != null && currentConfig.url != null) {
            return AcceleraAPI(currentConfig)
        }

        error(
            """
            API initialization failed.
            Missing configuration and no custom API provided by delegate.
            Set AcceleraConfig via configure(...) or implement AcceleraDelegate.customAPI.
            """.trimIndent()
        )

        return AcceleraAPIStub()
    }


    private fun configureBannersModule() {
        // Configure DivKit logger (similar to iOS DivKitLogger.setLogger)
        // Note: Android DivKit doesn't have direct logger API like iOS,
        // but errors are handled via ParsingErrorLogger in DivConfiguration
        log("Banners module configured")
    }
}
