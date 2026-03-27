package ai.accelera.library

import ai.accelera.library.api.AcceleraAPIProtocol
import ai.accelera.library.core.api.DefaultApiProvider
import ai.accelera.library.core.di.InternalModule
import kotlinx.serialization.json.Json

/**
 * Main library class for Accelera SDK.
 * Provides singleton access via [shared] instance.
 */
class Accelera private constructor() {
    private val internalModule = InternalModule()
    private val apiProvider = DefaultApiProvider { message -> error(message) }
    
    companion object {
        /**
         * Singleton instance to communicate with the library.
         */
        @JvmStatic
        val shared: Accelera = Accelera()
    }

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
        internalModule.logger.setDelegate(newDelegate)
    }

    /**
     * Configures the library with the provided configuration.
     * 
     * @param config Configuration object with API URL and token
     */
    fun configure(config: AcceleraConfig) {
        synchronized(this) {
            internalModule.configStore.setConfig(config)
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
        val updatedConfig = internalModule.configStore.updateUserInfo(userInfo) ?: run {
            error("Can't set userInfo — Accelera is not configured!")
            return
        }
        log("User info set to: ${updatedConfig.userInfo ?: "nil"}")
    }

    /**
     * Logs an event for user activity.
     * 
     * @param event Event data as JSON bytes
     */
    fun logEvent(event: ByteArray) {
        internalModule.eventActionExtractor.extract(event)?.let { delegate?.action(it) }

        val dataWithUserInfo = addUserInfo(to = event)
        
        // Log final data that will be sent to API
        try {
            val finalJsonString = dataWithUserInfo?.let { String(it, Charsets.UTF_8) } ?: "null"
            log("Logging event: $finalJsonString")
        } catch (e: Exception) {
            log("Logging event: <invalid UTF-8 data>")
        }
        
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
        internalModule.logger.log(msg)
    }

    internal fun error(error: Any) {
        val msg = "[Accelera] Error: $error"
        internalModule.logger.error(msg)
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
        val userInfo = internalModule.configStore.getConfig()?.userInfo
        return internalModule.payloadMerger.mergeUserInfo(payload = to, userInfo = userInfo)
    }

    private fun createApi(): AcceleraAPIProtocol {
        val currentConfig = internalModule.configStore.getConfig()
        return apiProvider.provide(config = currentConfig, delegate = delegate)
    }


    private fun configureBannersModule() {
        // Configure DivKit logger (similar to iOS DivKitLogger.setLogger)
        // Note: Android DivKit doesn't have direct logger API like iOS,
        // but errors are handled via ParsingErrorLogger in DivConfiguration
        log("Banners module configured")
    }
}
