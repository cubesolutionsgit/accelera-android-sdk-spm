package ai.accelera.library.banners.infrastructure.divkit

import android.content.Context
import ai.accelera.library.Accelera
import android.view.ContextThemeWrapper
import androidx.lifecycle.LifecycleOwner
import com.yandex.div.DivDataTag
import com.yandex.div.core.Div2Context
import com.yandex.div.core.DivConfiguration
import com.yandex.div.core.DivErrorsReporter
import com.yandex.div.core.view2.Div2View
import com.yandex.div.data.DivParsingEnvironment
import com.yandex.div.glide.GlideDivImageLoader
import com.yandex.div.json.ParsingErrorLogger
import com.yandex.div.lottie.DivLottieExtensionHandler
import com.yandex.div.lottie.DivLottieRawResProvider
import com.yandex.div.video.ExoDivPlayerFactory
import com.yandex.div2.DivData
import org.json.JSONObject

/**
 * Setup class for DivKit components.
 * Creates and configures Div2View instances with proper handlers.
 * Uses Glide for image loading as recommended by DivKit documentation.
 */
internal object DivKitSetup {

    /**
     * Creates a Div2View with configured components.
     *
     * @param context Android context
     * @param jsonData JSON data for DivKit content
     * @param lifecycleOwner LifecycleOwner for managing lifecycle (optional, can be null)
     * @return Configured Div2View instance
     */
    fun makeView(
        context: Context,
        jsonData: ByteArray,
        lifecycleOwner: LifecycleOwner? = null,
    ): Div2View {
        val configuration = createConfiguration(context, jsonData)

        val contextWrapper = when (context) {
            is ContextThemeWrapper -> context
            else -> ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault)
        }

        // Create Div2Context with proper constructor: (context, configuration, lifecycleOwner)
        val div2Context = Div2Context(
            baseContext = contextWrapper,
            configuration = configuration,
            lifecycleOwner = lifecycleOwner,
        )

        return Div2View(div2Context)
    }

    /**
     * Creates DivConfiguration with custom handlers.
     * ImageLoader is set via GlideImageLoader in the configuration.
     * Uses Application context for Glide to avoid FragmentActivity requirement.
     * Registers Lottie extension handler and ExoPlayer factory for video support.
     */
    private fun createConfiguration(
        context: Context,
        jsonData: ByteArray
    ): DivConfiguration {
        // Use Application context for Glide to avoid FragmentActivity requirement
        val appContext = context.applicationContext ?: context
        val imageLoader = GlideDivImageLoader(appContext)

        // Create Lottie raw resource provider (supports URL loading)
        val lottieRawResProvider = object : DivLottieRawResProvider {
            override fun provideRes(url: String): Int? {
                // Return null for URL-based resources (loaded from network)
                return null
            }

            override fun provideAssetFile(url: String): String? {
                // Return null for URL-based resources (loaded from network)
                return null
            }
        }

        // Create Lottie extension handler
        val lottieExtensionHandler = DivLottieExtensionHandler(lottieRawResProvider)

        // Create ExoPlayer factory for video support
        val exoPlayerFactory = ExoDivPlayerFactory(appContext)

        return DivConfiguration.Builder(imageLoader)
            .actionHandler(AcceleraUrlHandler(context, jsonData))
            .divErrorsReporter(createErrorLogger())
            .extension(lottieExtensionHandler)
            .divPlayerFactory(exoPlayerFactory)
            .build()
    }

    /**
     * Creates error logger for DivKit parsing errors.
     */
    private fun createErrorLogger(): DivErrorsReporter {
        return object : DivErrorsReporter {
            override fun onRuntimeError(
                divData: DivData?,
                divDataTag: DivDataTag,
                error: Throwable
            ) {
                Accelera.shared.error("DivKit parsing error: ${error.message}")
            }

            override fun onWarning(
                divData: DivData?,
                divDataTag: DivDataTag,
                warning: Throwable
            ) {
                Accelera.shared.error("DivKit parsing warning: ${warning.message}")
            }
        }
    }

    /**
     * Parses JSON data and creates DivData.
     * Handles both card format and direct div format.
     *
     * @param jsonData JSON bytes to parse
     * @return Parsed DivData or null if parsing fails
     */
    fun parseDivData(jsonData: ByteArray): DivData? {
        return runCatching {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val jsonObject = JSONObject(jsonString)

            // Check if it's a card format: {"card": {"div": {...}}}
            val divObject = jsonObject.optJSONObject("card")
                ?.let { card -> card.optJSONObject("div") ?: card }
                ?: jsonObject

            val environment = DivParsingEnvironment(ParsingErrorLogger.LOG)

            DivData(environment, divObject)
        }.onFailure { e ->
            Accelera.shared.error("Failed to parse DivData: ${e.message}")
        }.getOrNull()
    }
}

