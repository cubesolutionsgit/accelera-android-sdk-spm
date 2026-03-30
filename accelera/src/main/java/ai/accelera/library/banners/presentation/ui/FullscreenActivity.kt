package ai.accelera.library.banners.presentation.ui

import ai.accelera.library.banners.infrastructure.animation.StoryEntryAnimator
import ai.accelera.library.banners.infrastructure.gesture.StoryGestureHandler
import ai.accelera.library.banners.infrastructure.gesture.StoryGestureListener
import ai.accelera.library.banners.presentation.playback.PlaybackEvent
import ai.accelera.library.banners.presentation.playback.StoryPlaybackCoordinator
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity

class FullscreenActivity : AppCompatActivity() {

    private lateinit var jsonData: ByteArray
    private lateinit var rootLayout: FrameLayout
    private lateinit var progressContainer: ViewGroup

    private lateinit var gestureHandler: StoryGestureHandler
    private lateinit var activityAnimator: StoryEntryAnimator
    private lateinit var playbackCoordinator: StoryPlaybackCoordinator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jsonData = intent.getByteArrayExtra("jsonData") ?: return finish()
        val entryId = intent.getStringExtra("entryId") ?: return finish()

        setupUI()
        setupCoordinator(entryId)
        setupGestures()

        rootLayout.post {
            activityAnimator.animateOpen(rootLayout) {}
        }
    }

    private fun setupUI() {
        rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        progressContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                marginStart = (8 * resources.displayMetrics.density).toInt()
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
            isClickable = false
            isFocusable = false
            isFocusableInTouchMode = false
        }

        val closeButton = CloseButton(this).apply {
            setOnClickListener { playbackCoordinator.handleEvent(PlaybackEvent.CloseRequested) }
            layoutParams = FrameLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
                marginEnd = (16 * resources.displayMetrics.density).toInt()
                gravity = Gravity.TOP or Gravity.END
            }
        }

        rootLayout.addView(progressContainer)
        rootLayout.addView(closeButton)
        progressContainer.elevation = 10f
        closeButton.elevation = 20f

        setContentView(rootLayout)
        activityAnimator = StoryEntryAnimator(rootLayout)
    }

    private fun setupCoordinator(entryId: String) {
        playbackCoordinator = StoryPlaybackCoordinator(
            context = this,
            rootLayout = rootLayout,
            progressContainer = progressContainer,
            jsonData = jsonData,
            lifecycleOwner = this,
            onCloseRequested = ::closeStories
        )
        if (!playbackCoordinator.open(entryId)) {
            finish()
        }
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
        playbackCoordinator.handleEvent(PlaybackEvent.ActivityPause)
    }

    override fun onResume() {
        super.onResume()
        playbackCoordinator.handleEvent(PlaybackEvent.ActivityResume)
    }

    override fun onDestroy() {
        playbackCoordinator.handleEvent(PlaybackEvent.Destroyed)
        gestureHandler.cleanup()
        super.onDestroy()
    }
}

