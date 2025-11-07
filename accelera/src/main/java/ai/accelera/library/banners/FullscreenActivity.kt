package ai.accelera.library.banners

import ai.accelera.library.Accelera
import ai.accelera.library.utils.toJsonBytes
import android.animation.ValueAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.app.Activity
import androidx.viewpager2.widget.ViewPager2
import org.json.JSONObject
import kotlin.math.abs

/**
 * Fullscreen activity for displaying stories.
 */
class FullscreenActivity : Activity() {

    private lateinit var jsonData: ByteArray
    private var currentEntryId: String = ""
    private var entryIds: List<String> = emptyList()
    private var currentEntryIndex: Int = 0
    private var currentCards: List<JSONObject> = emptyList()
    private var currentCardIndex: Int = 0

    private lateinit var viewPager: ViewPager2
    private lateinit var progressContainer: ViewGroup
    private val progressBars = mutableListOf<StoryProgressBar>()
    private var progressAnimator: ValueAnimator? = null
    private var isPaused: Boolean = false
    private var pauseStartTime: Long = 0
    private var pausedDuration: Long = 0

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            if (!isPaused && currentCardIndex < progressBars.size) {
                updateProgress()
                handler.postDelayed(this, 16) // ~60fps
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        jsonData = intent.getByteArrayExtra("jsonData") ?: return finish()
        currentEntryId = intent.getStringExtra("entryId") ?: return finish()

        setupUI()
        loadEntryIds()
        loadEntry(currentEntryId)
    }

    private fun setupUI() {
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.BLACK)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Progress bars container
        progressContainer = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
                marginStart = (8 * resources.displayMetrics.density).toInt()
                marginEnd = (8 * resources.displayMetrics.density).toInt()
            }
        }

        // ViewPager for cards
        viewPager = ViewPager2(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            adapter = StoryCardAdapter()
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentCardIndex = position
                    showCard(position)
                }
            })
        }

        // Close button
        val closeButton = CloseButton(this).apply {
            setOnClickListener { finish() }
            layoutParams = FrameLayout.LayoutParams(
                (24 * resources.displayMetrics.density).toInt(),
                (24 * resources.displayMetrics.density).toInt()
            ).apply {
                topMargin = (16 * resources.displayMetrics.density).toInt()
                marginEnd = (16 * resources.displayMetrics.density).toInt()
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
            }
        }

        rootLayout.addView(viewPager)
        rootLayout.addView(progressContainer)
        rootLayout.addView(closeButton)

        setContentView(rootLayout)

        // Setup gesture detector for pause on long press
        val gestureDetector = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onLongPress(e: MotionEvent) {
                if (!isPaused) {
                    pauseProgress()
                }
            }
        })

        rootLayout.setOnTouchListener { _, event ->
            gestureDetector.onTouchEvent(event)
            if (event.action == android.view.MotionEvent.ACTION_UP && isPaused) {
                resumeProgress()
            }
            true
        }
    }

    private fun loadEntryIds() {
        try {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val fullscreens = root.optJSONObject("fullscreens")
            if (fullscreens != null) {
                entryIds = fullscreens.keys().asSequence().sorted().toList()
                currentEntryIndex = entryIds.indexOf(currentEntryId).coerceAtLeast(0)
            }
        } catch (e: Exception) {
            Accelera.shared.error("Failed to load entry IDs: ${e.message}")
        }
    }

    private fun loadEntry(id: String, lastCard: Boolean = false) {
        handler.removeCallbacks(progressUpdateRunnable)
        progressAnimator?.cancel()

        try {
            val jsonString = String(jsonData, Charsets.UTF_8)
            val root = JSONObject(jsonString)
            val fullscreens = root.optJSONObject("fullscreens")
            val entry = fullscreens?.optJSONObject(id) ?: run {
                finish()
                return
            }

            currentEntryId = id
            currentEntryIndex = entryIds.indexOf(id).coerceAtLeast(0)

            val cardsArray = entry.optJSONArray("cards")
            currentCards = if (cardsArray != null) {
                (0 until cardsArray.length()).map { cardsArray.getJSONObject(it) }
            } else {
                emptyList()
            }

            currentCardIndex = if (lastCard) currentCards.size - 1 else 0

            setupProgressBars()
            showCard(currentCardIndex)
        } catch (e: Exception) {
            Accelera.shared.error("Failed to load entry: ${e.message}")
            finish()
        }
    }

    private fun setupProgressBars() {
        progressContainer.removeAllViews()
        progressBars.clear()

        val hasDuration = currentCards.any { hasDuration(it) }
        if (!hasDuration) return

        val container = android.widget.LinearLayout(this).apply {
            orientation = android.widget.LinearLayout.HORIZONTAL
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        currentCards.forEach { _ ->
            val bar = StoryProgressBar(this).apply {
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    0,
                    (2 * resources.displayMetrics.density).toInt(),
                    1f
                ).apply {
                    marginEnd = (4 * resources.displayMetrics.density).toInt()
                }
            }
            container.addView(bar)
            progressBars.add(bar)
        }

        progressContainer.addView(container)
    }

    private fun hasDuration(card: JSONObject): Boolean {
        val cardObj = card.optJSONObject("card")
        val duration = cardObj?.optInt("duration") ?: card.optInt("duration")
        return duration > 0
    }

    private fun showCard(index: Int) {
        if (index < 0) {
            moveToPrevEntry()
            return
        }
        if (index >= currentCards.size) {
            moveToNextEntry()
            return
        }

        handler.removeCallbacks(progressUpdateRunnable)
        progressAnimator?.cancel()

        currentCardIndex = index

        // Update progress bars
        progressBars.forEachIndexed { i, bar ->
            bar.progress = if (i < index) 1f else 0f
        }

        // Update ViewPager
        (viewPager.adapter as? StoryCardAdapter)?.notifyDataSetChanged()
        viewPager.setCurrentItem(index, false)

        // Log view event
        try {
            val card = currentCards[index]
            val meta = card.optJSONObject("card")?.optJSONObject("meta") ?: JSONObject()
            val eventPayload = mapOf(
                "event" to "view",
                "meta" to meta.toString()
            )
            Accelera.shared.logEvent(eventPayload.toJsonBytes())
        } catch (e: Exception) {
            // Ignore
        }

        // Start progress animation if card has duration
        val card = currentCards[index]
        val duration = getCardDuration(card)
        if (duration > 0 && index < progressBars.size) {
            startProgressAnimation(index, duration)
        }
    }

    private fun getCardDuration(card: JSONObject): Long {
        val cardObj = card.optJSONObject("card")
        val duration = cardObj?.optInt("duration") ?: card.optInt("duration", 0)
        return if (duration > 0) duration.toLong() else 5000L // Default 5 seconds
    }

    private fun startProgressAnimation(cardIndex: Int, durationMs: Long) {
        if (cardIndex >= progressBars.size) return

        val bar = progressBars[cardIndex]
        progressAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = durationMs
            addUpdateListener { animator ->
                if (!isPaused) {
                    val progress = animator.animatedValue as Float
                    bar.progress = progress
                    if (progress >= 1f) {
                        nextCard()
                    }
                }
            }
            start()
        }
    }

    private fun updateProgress() {
        // This is called by the handler for smooth updates
        // The actual animation is handled by ValueAnimator
    }

    private fun pauseProgress() {
        if (isPaused) return
        isPaused = true
        pauseStartTime = System.currentTimeMillis()
        progressAnimator?.pause()
    }

    private fun resumeProgress() {
        if (!isPaused) return
        isPaused = false
        pausedDuration += System.currentTimeMillis() - pauseStartTime
        progressAnimator?.resume()
    }

    private fun nextCard() {
        showCard(currentCardIndex + 1)
    }

    private fun prevCard() {
        showCard(currentCardIndex - 1)
    }

    private fun moveToNextEntry() {
        if (currentEntryIndex + 1 < entryIds.size) {
            loadEntry(entryIds[currentEntryIndex + 1])
        } else {
            finish()
        }
    }

    private fun moveToPrevEntry() {
        if (currentEntryIndex - 1 >= 0) {
            loadEntry(entryIds[currentEntryIndex - 1], lastCard = true)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(progressUpdateRunnable)
        progressAnimator?.cancel()
    }

    private inner class StoryCardAdapter : androidx.recyclerview.widget.RecyclerView.Adapter<StoryCardViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryCardViewHolder {
            val divView = DivKitSetup.makeView(this@FullscreenActivity, jsonData)
            // ViewPager2 requires pages to fill the whole ViewPager2 (use match_parent)
            divView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            return StoryCardViewHolder(divView)
        }

        override fun onBindViewHolder(holder: StoryCardViewHolder, position: Int) {
            if (position < currentCards.size) {
                val card = currentCards[position]
                val cardBytes = card.toString().toByteArray(Charsets.UTF_8)
                val divData = DivKitSetup.parseDivData(cardBytes)
                if (divData != null) {
                    val tag = com.yandex.div.DivDataTag("story_${currentEntryId}_$position")
                    holder.divView.setData(divData, tag)
                }
            }
        }

        override fun getItemCount(): Int = currentCards.size
    }

    private class StoryCardViewHolder(val divView: com.yandex.div.core.view2.Div2View) :
        androidx.recyclerview.widget.RecyclerView.ViewHolder(divView)
}

