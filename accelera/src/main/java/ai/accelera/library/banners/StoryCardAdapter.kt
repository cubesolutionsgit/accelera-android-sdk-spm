package ai.accelera.library.banners

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.yandex.div.DivDataTag
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

/**
 * Adapter for story cards in ViewPager2.
 */
class StoryCardAdapter(
    private val jsonData: ByteArray,
    val entryId: String,
    val cards: List<JSONObject>,
    private val makeDivView: () -> Div2View
) : RecyclerView.Adapter<StoryCardAdapter.StoryCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoryCardViewHolder {
        val divView = makeDivView()
        // ViewPager2 requires pages to fill the whole ViewPager2 (use match_parent)
        divView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        return StoryCardViewHolder(divView)
    }

    override fun onBindViewHolder(holder: StoryCardViewHolder, position: Int) {
        if (position < cards.size) {
            val card = cards[position]
            val cardBytes = card.toString().toByteArray(Charsets.UTF_8)
            val divData = DivKitSetup.parseDivData(cardBytes)
            if (divData != null) {
                val tag = DivDataTag("story_${entryId}_$position")
                holder.divView.setData(divData, tag)
            }
        }
    }

    override fun getItemCount(): Int = cards.size

    class StoryCardViewHolder(val divView: Div2View) : RecyclerView.ViewHolder(divView)
}

