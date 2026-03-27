package ai.accelera.library.banners.presentation.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import com.yandex.div.DivDataTag

import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

/**
 * Custom ViewGroup for managing story cards within a single entry.
 * Handles card visibility, animations, and lifecycle of Div2View instances.
 *
 * Follows Single Responsibility Principle - only manages cards for current entry.
 */
class StoryCardContainerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val cardViews = mutableMapOf<Int, Div2View>()
    private var currentCardIndex = -1
    private var jsonData: ByteArray? = null
    private var entryId: String = ""

    init {
        // Don't intercept touch events - let parent handle gestures
        isClickable = false
        isFocusable = false
        isFocusableInTouchMode = false
    }

    /**
     * Never intercept touch events - always let parent handle them.
     * This ensures gesture handler always receives events.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        return false  // Never intercept - let parent handle
    }

    /**
     * Never handle touch events - always let parent handle them.
     */
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return false  // Never handle - let parent handle
    }

    /**
     * Sets up cards for the entry.
     * Creates Div2View instances for all cards and prepares them.
     */
    fun setupCards(
        cards: List<JSONObject>,
        entryId: String,
        jsonData: ByteArray,
        makeDivView: () -> Div2View
    ) {
        this.jsonData = jsonData
        this.entryId = entryId

        cardViews.values.forEach { divView ->
            try {
                // Temporary deactivation: keep players alive for possible reuse.
                DivKitSetup.pauseVideoPlayers(divView)
                divView.clearAnimation()
                divView.animate()?.cancel()
                divView.setOnTouchListener(null)
                (divView.parent as? ViewGroup)?.removeView(divView)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
        cardViews.clear()

        // Create and add all card views (initially hidden)
        cards.forEachIndexed { index, card ->
            val divView = makeDivView()
            divView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )

            // Load card data
            val cardBytes = card.toString().toByteArray(Charsets.UTF_8)
            val divData = DivKitSetup.parseDivData(cardBytes)
            if (divData != null) {
                val tag = DivDataTag("story_${entryId}_$index")
                divView.setData(divData, tag)
            }
            // Prevent hidden pre-created cards from playing audio before activation.
            DivKitSetup.pauseVideoPlayers(divView)

            // Don't intercept touch events - let parent handle gestures
            divView.isClickable = false
            divView.isFocusable = false
            divView.isFocusableInTouchMode = false

            // Initially hidden
            divView.visibility = View.GONE
            divView.alpha = 0f

            addView(divView)
            cardViews[index] = divView
        }
    }

    /**
     * Sets up cards using pre-created Div2View instances.
     * Used for preloaded entries to reuse views.
     */
    fun setupCardsWithViews(
        cards: List<JSONObject>,
        entryId: String,
        jsonData: ByteArray,
        cachedViews: Map<Int, Div2View>
    ) {
        this.jsonData = jsonData
        this.entryId = entryId

        cardViews.values.forEach { divView ->
            try {
                // Temporary deactivation: keep players alive for possible reuse.
                DivKitSetup.pauseVideoPlayers(divView)
                divView.clearAnimation()
                divView.animate()?.cancel()
                divView.setOnTouchListener(null)
                (divView.parent as? ViewGroup)?.removeView(divView)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
        cardViews.clear()

        cards.forEachIndexed { index, card ->
            val isCached = cachedViews.containsKey(index)
            val divView = cachedViews[index] ?: DivKitSetup.makeView(context, jsonData)

            (divView.parent as? ViewGroup)?.removeView(divView)

            divView.layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
            )

            // Only set data on newly created views; cached views already have data from preloading
            if (!isCached) {
                val cardBytes = card.toString().toByteArray(Charsets.UTF_8)
                val divData = DivKitSetup.parseDivData(cardBytes)
                if (divData != null) {
                    val tag = DivDataTag("story_${entryId}_$index")
                    divView.setData(divData, tag)
                }
            }
            // Keep hidden cards silent until explicitly activated.
            DivKitSetup.pauseVideoPlayers(divView)

            divView.isClickable = false
            divView.isFocusable = false
            divView.isFocusableInTouchMode = false
            divView.setOnTouchListener { _, _ -> false }

            divView.visibility = View.GONE
            divView.alpha = 0f

            addView(divView)
            cardViews[index] = divView
        }
    }

    /**
     * Shows a specific card with optional animation.
     */
    fun showCard(index: Int, animate: Boolean = true, restartPlayback: Boolean = true) {
        if (index < 0 || index >= cardViews.size) return

        val targetView = cardViews[index] ?: return

        if (currentCardIndex >= 0 && currentCardIndex != index) {
            val currentView = cardViews[currentCardIndex]
            if (currentView != null) {
                DivKitSetup.pauseVideoPlayers(currentView)
                currentView.clearAnimation()
                currentView.animate()?.cancel()

                if (animate) {
                    currentView.animate()
                        .alpha(0f)
                        .setDuration(200L)
                        .withEndAction {
                            currentView.visibility = View.GONE
                        }
                        .start()
                } else {
                    currentView.visibility = View.GONE
                    currentView.alpha = 0f
                }
            }
        }

        targetView.visibility = View.VISIBLE
        if (animate && currentCardIndex >= 0) {
            targetView.alpha = 0f
            targetView.animate()
                .alpha(1f)
                .setDuration(200L)
                .start()
        } else {
            targetView.alpha = 1f
        }

        if (restartPlayback) {
            DivKitSetup.restartVideoPlayers(targetView)
        } else {
            DivKitSetup.playVideoPlayers(targetView)
        }
        currentCardIndex = index
    }

    /**
     * Gets the current card index.
     */
    fun getCurrentCardIndex(): Int = currentCardIndex

    /**
     * Gets the number of cards.
     */
    fun getCardCount(): Int = cardViews.size

    /**
     * Gets the Div2View for a specific card index.
     */
    fun getCardView(index: Int): Div2View? = cardViews[index]

    /**
     * Pauses all video/audio playback in this container.
     * Players stay alive and can be resumed.
     */
    fun pauseVideoPlayers() {
        cardViews.values.forEach { divView ->
            try {
                DivKitSetup.pauseVideoPlayers(divView)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    /**
     * Permanently releases all ExoPlayer instances in this container.
     * Use only when the container is being discarded.
     */
    fun releaseVideoPlayers() {
        cardViews.values.forEach { divView ->
            try {
                DivKitSetup.releaseVideoPlayers(divView)
            } catch (e: Exception) {
                // Ignore errors
            }
        }
    }

    /**
     * Cleans up resources.
     */
    fun cleanup() {
        cardViews.values.forEach { divView ->
            try {
                DivKitSetup.releaseVideoPlayers(divView)
                divView.clearAnimation()
                divView.animate()?.cancel()
                divView.setOnTouchListener(null)
            } catch (e: Exception) {
                // Ignore errors during cleanup
            }
        }

        // Remove all views from parent
        cardViews.values.forEach { removeView(it) }

        // Clear all references
        cardViews.clear()
        currentCardIndex = -1
        jsonData = null
        entryId = ""
    }
}
