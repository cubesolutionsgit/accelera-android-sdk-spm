package ai.accelera.library.banners.infrastructure.divkit

import android.content.Context
import ai.accelera.library.Accelera
import ai.accelera.library.banners.AcceleraAttachedContentContext
import ai.accelera.library.utils.normalizedDivDataObjectOrNull
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
 * - [restartAll]: rewind to the beginning and play (use when re-showing a card)
 * - [releaseAll]: permanent destruction (use only when Div2View is discarded)
 *
 * It also tracks the current playback position of every player (via
 * [DivPlayer.Observer.onCurrentTimeChange]) so playback can survive a hosting
 * activity recreation: [capturePositionsMs] snapshots positions before the old
 * view is destroyed, [applyPositionsMs] seeks the recreated players back. Because
 * DivKit creates players lazily while binding the new view, positions that cannot
 * be applied immediately are kept pending and consumed in [makePlayer] as the
 * players appear, matched by creation order (deterministic for identical JSON).
 */
private class TrackingPlayerFactory(context: Context) : DivPlayerFactory {

    private class TrackedPlayer(val player: DivPlayer) {
        @Volatile
        var lastPositionMs: Long = 0
    }

    private val delegate = ExoDivPlayerFactory(context)
    private val trackedPlayers = mutableListOf<TrackedPlayer>()
    private var pendingSeekPositionsMs: MutableList<Long?>? = null

    override fun makePlayer(src: List<DivVideoSource>, config: DivPlayerPlaybackConfig): DivPlayer {
        val player = delegate.makePlayer(src, config)
        val tracked = TrackedPlayer(player)
        player.addObserver(object : DivPlayer.Observer {
            override fun onCurrentTimeChange(timeMs: Long) {
                tracked.lastPositionMs = timeMs
            }

            override fun onEnd() {
                tracked.lastPositionMs = 0
            }
        })
        trackedPlayers.add(tracked)
        consumePendingSeek(trackedPlayers.size - 1, tracked)
        return player
    }

    override fun makePlayerView(context: Context): DivPlayerView = delegate.makePlayerView(context)

    override fun makePreloader(): DivPlayerPreloader = delegate.makePreloader()

    fun pauseAll() {
        trackedPlayers.forEach { runCatching { it.player.pause() } }
    }

    fun playAll() {
        trackedPlayers.forEach { runCatching { it.player.play() } }
    }

    fun restartAll() {
        trackedPlayers.forEach {
            runCatching {
                it.player.seek(0)
                it.lastPositionMs = 0
                it.player.play()
            }
        }
    }

    fun releaseAll() {
        trackedPlayers.forEach { runCatching { it.player.release() } }
        trackedPlayers.clear()
        pendingSeekPositionsMs = null
    }

    /**
     * Returns the last known playback position of every player, in creation order.
     */
    fun capturePositionsMs(): List<Long> = trackedPlayers.map { it.lastPositionMs }

    /**
     * Seeks players back to previously captured positions. Positions for players
     * that don't exist yet are applied later, when [makePlayer] creates them.
     */
    fun applyPositionsMs(positionsMs: List<Long>) {
        pendingSeekPositionsMs = positionsMs.mapTo(mutableListOf()) { it }
        trackedPlayers.forEachIndexed { index, tracked -> consumePendingSeek(index, tracked) }
    }

    private fun consumePendingSeek(index: Int, tracked: TrackedPlayer) {
        val pending = pendingSeekPositionsMs ?: return
        val positionMs = pending.getOrNull(index) ?: return
        pending[index] = null
        if (positionMs > 0) {
            runCatching {
                tracked.player.seek(positionMs)
                tracked.lastPositionMs = positionMs
            }
        }
    }
}

/**
 * Setup class for DivKit components.
 * Creates and configures Div2View instances with proper handlers.
 * Uses Glide for image loading as recommended by DivKit documentation.
 */
internal object DivKitSetup {

    /**
     * Weak keys: a Div2View whose owner never calls [releaseVideoPlayers]
     * (e.g. discarded without cleanup) must not be pinned in memory by this map.
     */
    private val playerFactories = java.util.WeakHashMap<Div2View, TrackingPlayerFactory>()

    /**
     * Creates a Div2View with configured components.
     *
     * @param context Android context
     * @param jsonData JSON data for DivKit content
     * @param lifecycleOwner LifecycleOwner for managing lifecycle (optional, can be null)
     * @return Configured Div2View instance
     */
    /**
     * Returns true when [jsonData] contains content that can actually be rendered.
     * Use this to decide whether to show a banner at all — an empty placeholder
     * should never be attached when there is nothing to display.
     */
    fun hasDisplayableContent(jsonData: ByteArray): Boolean = parseDivData(jsonData) != null

    /**
     * Builds a ready-to-show [Div2View] with parsed data already applied.
     *
     * Returns null (and logs) when there is no displayable content or when DivKit
     * view creation throws — so callers can skip rendering instead of attaching an
     * empty banner or letting an exception reach the host app.
     */
    fun makeBoundViewOrNull(
        context: Context,
        jsonData: ByteArray,
        tag: DivDataTag,
        lifecycleOwner: LifecycleOwner? = null,
        originContext: AcceleraAttachedContentContext? = null,
        variableScope: AcceleraDivVariableScope? = null,
    ): Div2View? {
        val divData = parseDivData(jsonData) ?: run {
            Accelera.shared.log("No content to display")
            return null
        }
        return runCatching {
            val view = makeView(context, jsonData, lifecycleOwner, originContext, variableScope)
            view.setData(divData, tag)
            view
        }.onFailure { e ->
            Accelera.shared.error("Failed to create DivView: ${e.message}")
        }.getOrNull()
    }

    fun makeView(
        context: Context,
        jsonData: ByteArray,
        lifecycleOwner: LifecycleOwner? = null,
        originContext: AcceleraAttachedContentContext? = null,
        variableScope: AcceleraDivVariableScope? = null,
    ): Div2View {
        val appContext = context.applicationContext ?: context
        val trackingFactory = TrackingPlayerFactory(appContext)
        val configuration = createConfiguration(
            context = context,
            jsonData = jsonData,
            playerFactory = trackingFactory,
            originContext = originContext,
            variableScope = variableScope
        )

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
     * Snapshots the current playback position (ms) of every player in a Div2View,
     * in player creation order. Use before the hosting activity is recreated.
     */
    fun captureVideoPositions(view: Div2View): List<Long> {
        return playerFactories[view]?.capturePositionsMs() ?: emptyList()
    }

    /**
     * Seeks a Div2View's players back to previously captured positions.
     * Players that DivKit has not created yet are seeked as they appear.
     */
    fun applyVideoPositions(view: Div2View, positionsMs: List<Long>) {
        if (positionsMs.isEmpty()) return
        playerFactories[view]?.applyPositionsMs(positionsMs)
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
        playerFactory: TrackingPlayerFactory,
        originContext: AcceleraAttachedContentContext?,
        variableScope: AcceleraDivVariableScope?
    ): DivConfiguration {
        val appContext = context.applicationContext ?: context
        val imageLoader = GlideDivImageLoader(appContext)

        val lottieRawResProvider = object : DivLottieRawResProvider {
            override fun provideRes(url: String): Int? = null
            override fun provideAssetFile(url: String): String? = null
        }

        val lottieExtensionHandler = DivLottieExtensionHandler(lottieRawResProvider)

        val builder = DivConfiguration.Builder(imageLoader)
            .actionHandler(AcceleraUrlHandler(context, jsonData, originContext))
            .divErrorsReporter(createErrorLogger())
            .extension(lottieExtensionHandler)
            .divPlayerFactory(playerFactory)

        if (variableScope != null) {
            builder.divVariableController(variableScope.divVariableController)
        }

        return builder.build()
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

            val divObject = jsonObject.normalizedDivDataObjectOrNull() ?: run {
                Accelera.shared.log("No DivKit card data in content response")
                return null
            }

            val environment = DivParsingEnvironment(ParsingErrorLogger.LOG)

            DivData(environment, divObject)
        }.onFailure { e ->
            Accelera.shared.error("Failed to parse DivData: ${e.message}")
        }.getOrNull()
    }
}

