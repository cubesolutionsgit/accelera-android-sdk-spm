package ai.accelera.library.banners.presentation.ui

import ai.accelera.library.Accelera
import ai.accelera.library.banners.infrastructure.activity.AcceleraActivityTracker
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.meta
import ai.accelera.library.utils.toJsonBytes
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.yandex.div.DivDataTag
import com.yandex.div.core.view2.Div2View
import org.json.JSONObject

class PopupActivity : AppCompatActivity() {
    private lateinit var jsonData: ByteArray
    private var divView: Div2View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AcceleraActivityTracker.note(this)

        jsonData = intent.getByteArrayExtra(EXTRA_JSON_DATA) ?: return finish()

        window.setBackgroundDrawableResource(android.R.color.transparent)
        setupContent()
    }

    private fun setupContent() {
        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(rootLayout)

        val view = DivKitSetup.makeView(this, jsonData, this)
        divView = view
        rootLayout.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        DivKitSetup.parseDivData(jsonData)?.let { divData ->
            view.setData(divData, DivDataTag("accelera_popup_${System.currentTimeMillis()}"))
        }

        logView()

        if (jsonData.closable != false) {
            addCloseButton(rootLayout)
        }
    }

    private fun addCloseButton(rootLayout: FrameLayout) {
        val density = resources.displayMetrics.density
        val closeButton = CloseButton(this).apply {
            setOnClickListener {
                logClose()
                finish()
            }
            layoutParams = FrameLayout.LayoutParams(
                (24 * density).toInt(),
                (24 * density).toInt()
            ).apply {
                topMargin = (28 * density).toInt()
                marginEnd = (16 * density).toInt()
                gravity = Gravity.TOP or Gravity.END
            }
        }
        rootLayout.addView(closeButton)
        closeButton.elevation = 20f
    }

    private fun logView() {
        val meta = jsonData.meta ?: emptyMap<String, Any?>()
        Accelera.shared.logEvent(mapOf("event" to "view", "meta" to meta).toJsonBytes())
    }

    private fun logClose() {
        val meta = (jsonData.meta as? JSONObject) ?: JSONObject()
        Accelera.shared.logEvent(mapOf("event" to "close", "meta" to meta).toJsonBytes())
    }

    override fun onDestroy() {
        divView?.let { DivKitSetup.releaseVideoPlayers(it) }
        divView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_JSON_DATA = "ai.accelera.library.extra.JSON_DATA"
    }
}
