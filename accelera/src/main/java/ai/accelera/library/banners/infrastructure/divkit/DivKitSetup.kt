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
import com.yandex.div.core.player.DivPlayer
import com.yandex.div.core.player.DivPlayerFactory
import com.yandex.div.core.player.DivPlayerPlaybackConfig
import com.yandex.div.core.player.DivPlayerPreloader
import com.yandex.div.core.player.DivPlayerView
import com.yandex.div.core.player.DivVideoSource
import com.yandex.div.video.ExoDivPlayerFactory
import com.yandex.div2.DivData
import org.json.JSONObject

/**
 * Wraps [ExoDivPlayerFactory] to track created [DivPlayer] instances,
 * enabling pause/resume/release of video/audio playback on demand.
 *
 * - [pauseAll]: temporary stop, player stays alive (use when hiding a card)
 * - [playAll]: resume after pause (use when showing a card)
 * - [releaseAll]: permanent destruction (use only when Div2View is discarded)
 */
private class TrackingPlayerFactory(context: Context) : DivPlayerFactory {
    private val delegate = ExoDivPlayerFactory(context)
    private val activePlayers = mutableListOf<DivPlayer>()

    override fun makePlayer(src: List<DivVideoSource>, config: DivPlayerPlaybackConfig): DivPlayer {
        val player = delegate.makePlayer(src, config)
        activePlayers.add(player)
        return player
    }

    override fun makePlayerView(context: Context): DivPlayerView = delegate.makePlayerView(context)

    override fun makePreloader(): DivPlayerPreloader = delegate.makePreloader()

    fun pauseAll() {
        activePlayers.forEach { runCatching { it.pause() } }
    }

    fun playAll() {
        activePlayers.forEach { runCatching { it.play() } }
    }

    fun restartAll() {
        activePlayers.forEach {
            runCatching {
                it.seek(0)
                it.play()
            }
        }
    }

    fun releaseAll() {
        activePlayers.forEach { runCatching { it.release() } }
        activePlayers.clear()
    }
}

/**
 * Setup class for DivKit components.
 * Creates and configures Div2View instances with proper handlers.
 * Uses Glide for image loading as recommended by DivKit documentation.
 */
internal object DivKitSetup {

    private val playerFactories = mutableMapOf<Div2View, TrackingPlayerFactory>()

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
        val appContext = context.applicationContext ?: context
        val trackingFactory = TrackingPlayerFactory(appContext)
        val configuration = createConfiguration(context, jsonData, trackingFactory)

        val contextWrapper = when (context) {
            is ContextThemeWrapper -> context
            else -> ContextThemeWrapper(context, android.R.style.Theme_DeviceDefault)
        }

        val div2Context = Div2Context(
            baseContext = contextWrapper,
            configuration = configuration,
            lifecycleOwner = lifecycleOwner,
        )

        val view = Div2View(div2Context)
        playerFactories[view] = trackingFactory
        return view
    }

    /**
     * Pauses all players for a Div2View. The players stay alive
     * and can be resumed later with [playVideoPlayers].
     */
    fun pauseVideoPlayers(view: Div2View) {
        playerFactories[view]?.pauseAll()
    }

    /**
     * Resumes playback on all players for a Div2View from current position.
     */
    fun playVideoPlayers(view: Div2View) {
        playerFactories[view]?.playAll()
    }

    /**
     * Restarts playback from the beginning for all players in a Div2View.
     */
    fun restartVideoPlayers(view: Div2View) {
        playerFactories[view]?.restartAll()
    }

    /**
     * Permanently releases all ExoPlayer instances for a Div2View.
     * Use only when the view is being discarded and will never be shown again.
     */
    fun releaseVideoPlayers(view: Div2View) {
        playerFactories.remove(view)?.releaseAll()
    }

    /**
     * Creates DivConfiguration with custom handlers.
     * ImageLoader is set via GlideImageLoader in the configuration.
     * Uses Application context for Glide to avoid FragmentActivity requirement.
     * Registers Lottie extension handler and ExoPlayer factory for video support.
     */
    private fun createConfiguration(
        context: Context,
        jsonData: ByteArray,
        playerFactory: TrackingPlayerFactory
    ): DivConfiguration {
        val appContext = context.applicationContext ?: context
        val imageLoader = GlideDivImageLoader(appContext)

        val lottieRawResProvider = object : DivLottieRawResProvider {
            override fun provideRes(url: String): Int? = null
            override fun provideAssetFile(url: String): String? = null
        }

        val lottieExtensionHandler = DivLottieExtensionHandler(lottieRawResProvider)

        return DivConfiguration.Builder(imageLoader)
            .actionHandler(AcceleraUrlHandler(context, jsonData))
            .divErrorsReporter(createErrorLogger())
            .extension(lottieExtensionHandler)
            .divPlayerFactory(playerFactory)
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

