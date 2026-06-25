package ai.accelera.library.banners.presentation.ui

import ai.accelera.library.Accelera
import ai.accelera.library.banners.infrastructure.animation.StoryEntryAnimator
import ai.accelera.library.banners.infrastructure.divkit.AcceleraDivVariableScope
import ai.accelera.library.banners.infrastructure.divkit.AcceleraScopeRegistry
import ai.accelera.library.banners.infrastructure.gesture.StoryGestureHandler
import ai.accelera.library.banners.infrastructure.gesture.StoryGestureListener
import ai.accelera.library.banners.presentation.playback.PlaybackEvent
import ai.accelera.library.banners.presentation.playback.StoryPlaybackCoordinator
import ai.accelera.library.core.constants.AcceleraDimens
import ai.accelera.library.utils.dpToPx
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams

class FullscreenActivity : AppCompatActivity() {

    private lateinit var jsonData: ByteArray
    private lateinit var rootLayout: FrameLayout
    private lateinit var progressContainer: ViewGroup

    private lateinit var gestureHandler: StoryGestureHandler
    private lateinit var activityAnimator: StoryEntryAnimator
    private lateinit var playbackCoordinator: StoryPlaybackCoordinator
    private var scopeToken: String? = null
    private var variableScope: AcceleraDivVariableScope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jsonData = intent.getByteArrayExtra(EXTRA_JSON_DATA) ?: return finish()
        val entryId = intent.getStringExtra(EXTRA_ENTRY_ID) ?: return finish()
        scopeToken = intent.getStringExtra(EXTRA_SCOPE_TOKEN)
        variableScope = AcceleraScopeRegistry.get(scopeToken)

        // Guard the whole setup chain (DivKit view creation included) so the SDK
        // never crashes the host app; close the screen on any failure instead.
        val started = runCatching {
            setupUI()
            val opened = setupCoordinator(entryId)
            if (opened) {
                setupGestures()
                rootLayout.post { activityAnimator.animateOpen(rootLayout) {} }
            }
            opened
        }.onFailure { Accelera.shared.error("Failed to open stories: ${it.message}") }
            .getOrDefault(false)

        if (!started) finish()
    }

    private fun setupUI() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val progressBaseTop = dpToPx(AcceleraDimens.PROGRESS_TOP_MARGIN_DP)
        val closeBaseTop = dpToPx(AcceleraDimens.FULLSCREEN_CLOSE_TOP_MARGIN_DP)

        progressContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = progressBaseTop
                marginStart = dpToPx(AcceleraDimens.PROGRESS_SIDE_MARGIN_DP)
                marginEnd = dpToPx(AcceleraDimens.PROGRESS_SIDE_MARGIN_DP)
            }
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }

        val closeButton = CloseButton(this).apply {
            setOnClickListener { playbackCoordinator.handleEvent(PlaybackEvent.CloseRequested) }
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(AcceleraDimens.CLOSE_BUTTON_SIZE_DP),
                dpToPx(AcceleraDimens.CLOSE_BUTTON_SIZE_DP)
            ).apply {
                topMargin = closeBaseTop
                marginEnd = dpToPx(AcceleraDimens.FULLSCREEN_CLOSE_END_MARGIN_DP)
                gravity = Gravity.TOP or Gravity.END
            }
        }

        rootLayout.addView(progressContainer)
        rootLayout.addView(closeButton)
        progressContainer.elevation = AcceleraDimens.PROGRESS_CONTAINER_ELEVATION
        closeButton.elevation = AcceleraDimens.CLOSE_BUTTON_ELEVATION

        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { _, insets ->
            val statusTop = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            progressContainer.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = statusTop + progressBaseTop
            }
            closeButton.updateLayoutParams<FrameLayout.LayoutParams> {
                topMargin = statusTop + closeBaseTop
            }
            insets
        }

        setContentView(rootLayout)
        activityAnimator = StoryEntryAnimator(rootLayout)
    }

    private fun setupCoordinator(entryId: String): Boolean {
        playbackCoordinator = StoryPlaybackCoordinator(
            context = this,
            rootLayout = rootLayout,
            progressContainer = progressContainer,
            jsonData = jsonData,
            lifecycleOwner = this,
            variableScope = variableScope,
            onCloseRequested = ::closeStories
        )
        return playbackCoordinator.open(entryId)
    }

    private fun setupGestures() {
        gestureHandler = StoryGestureHandler(
            context = this,
            rootView = rootLayout,
            listener = object : StoryGestureListener {
                override fun onTapLeft() = playbackCoordinator.handleEvent(PlaybackEvent.TapPrev)
                override fun onTapRight() = playbackCoordinator.handleEvent(PlaybackEvent.TapNext)
                override fun onSwipeLeft() = playbackCoordinator.handleEvent(PlaybackEvent.SwipeNextEntry)
                override fun onSwipeRight() = playbackCoordinator.handleEvent(PlaybackEvent.SwipePrevEntry)
                override fun onSwipeDown() = playbackCoordinator.handleEvent(PlaybackEvent.CloseRequested)
                override fun onLongPress() = playbackCoordinator.handleEvent(PlaybackEvent.LongPressStart)
                override fun onLongPressEnd() = playbackCoordinator.handleEvent(PlaybackEvent.LongPressEnd)
            }
        )
        gestureHandler.setupTouchListener()
    }

    /**
     * Called by [ai.accelera.library.banners.infrastructure.divkit.AcceleraUrlHandler] when a
     * "link" action is triggered from within this screen and the delegate requests auto-dismiss.
     * Triggers the same close animation as the X button / swipe-down gesture.
     */
    internal fun requestClose() {
        closeStories()
    }

    private fun closeStories() {
        activityAnimator.animateClose(rootLayout) {
            finish()
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    override fun onPause() {
        super.onPause()
        if (::playbackCoordinator.isInitialized) {
            playbackCoordinator.handleEvent(PlaybackEvent.ActivityPause)
        }
    }

    override fun onResume() {
        super.onResume()
        if (::playbackCoordinator.isInitialized) {
            playbackCoordinator.handleEvent(PlaybackEvent.ActivityResume)
        }
    }

    override fun onDestroy() {
        if (::playbackCoordinator.isInitialized) {
            playbackCoordinator.handleEvent(PlaybackEvent.Destroyed)
        }
        if (::gestureHandler.isInitialized) {
            gestureHandler.cleanup()
        }
        AcceleraScopeRegistry.remove(scopeToken)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_JSON_DATA = "ai.accelera.library.extra.JSON_DATA"
        const val EXTRA_ENTRY_ID = "ai.accelera.library.extra.ENTRY_ID"
        const val EXTRA_SCOPE_TOKEN = "ai.accelera.library.extra.SCOPE_TOKEN"
    }
}

