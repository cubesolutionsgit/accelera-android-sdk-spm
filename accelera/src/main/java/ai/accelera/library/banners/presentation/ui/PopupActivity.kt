package ai.accelera.library.banners.presentation.ui

import ai.accelera.library.Accelera
import ai.accelera.library.banners.infrastructure.activity.AcceleraActivityTracker
import ai.accelera.library.banners.infrastructure.divkit.AcceleraDivVariableScope
import ai.accelera.library.banners.infrastructure.divkit.AcceleraScopeRegistry
import ai.accelera.library.banners.infrastructure.divkit.DivKitSetup
import ai.accelera.library.core.constants.AcceleraDimens
import ai.accelera.library.core.constants.AcceleraEvents
import ai.accelera.library.core.constants.AcceleraJsonKeys
import ai.accelera.library.utils.closable
import ai.accelera.library.utils.dpToPx
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
    private var scopeToken: String? = null
    private var variableScope: AcceleraDivVariableScope? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AcceleraActivityTracker.note(this)

        jsonData = intent.getByteArrayExtra(EXTRA_JSON_DATA) ?: return finish()
        scopeToken = intent.getStringExtra(EXTRA_SCOPE_TOKEN)
        variableScope = AcceleraScopeRegistry.get(scopeToken)

        window.setBackgroundDrawableResource(android.R.color.transparent)

        // Never let DivKit setup crash the host app; close the popup instead.
        val ready = runCatching { setupContent() }
            .onFailure { Accelera.shared.error("Failed to present popup: ${it.message}") }
            .getOrDefault(false)
        if (!ready) finish()
    }

    /** Returns true when content was rendered; false means nothing to show. */
    private fun setupContent(): Boolean {
        val tag = DivDataTag("accelera_popup_${System.currentTimeMillis()}")
        val view = DivKitSetup.makeBoundViewOrNull(
            context = this,
            jsonData = jsonData,
            tag = tag,
            lifecycleOwner = this,
            variableScope = variableScope
        ) ?: return false

        val rootLayout = FrameLayout(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(rootLayout)

        divView = view
        rootLayout.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )

        logView()

        if (jsonData.closable != false) {
            addCloseButton(rootLayout)
        }
        return true
    }

    private fun addCloseButton(rootLayout: FrameLayout) {
        val closeButton = CloseButton(this).apply {
            setOnClickListener {
                logClose()
                finish()
            }
            layoutParams = FrameLayout.LayoutParams(
                dpToPx(AcceleraDimens.CLOSE_BUTTON_SIZE_DP),
                dpToPx(AcceleraDimens.CLOSE_BUTTON_SIZE_DP)
            ).apply {
                topMargin = dpToPx(AcceleraDimens.POPUP_CLOSE_TOP_MARGIN_DP)
                marginEnd = dpToPx(AcceleraDimens.POPUP_CLOSE_END_MARGIN_DP)
                gravity = Gravity.TOP or Gravity.END
            }
        }
        rootLayout.addView(closeButton)
        closeButton.elevation = AcceleraDimens.CLOSE_BUTTON_ELEVATION
    }

    private fun logView() {
        val meta = jsonData.meta ?: emptyMap<String, Any?>()
        Accelera.shared.logEvent(
            mapOf(AcceleraJsonKeys.EVENT to AcceleraEvents.VIEW, AcceleraJsonKeys.META to meta).toJsonBytes()
        )
    }

    private fun logClose() {
        val meta = (jsonData.meta as? JSONObject) ?: JSONObject()
        Accelera.shared.logEvent(
            mapOf(AcceleraJsonKeys.EVENT to AcceleraEvents.CLOSE, AcceleraJsonKeys.META to meta).toJsonBytes()
        )
    }

    override fun onDestroy() {
        divView?.let { DivKitSetup.releaseVideoPlayers(it) }
        divView = null
        AcceleraScopeRegistry.remove(scopeToken)
        super.onDestroy()
    }

    companion object {
        const val EXTRA_JSON_DATA = "ai.accelera.library.extra.JSON_DATA"
        const val EXTRA_SCOPE_TOKEN = "ai.accelera.library.extra.SCOPE_TOKEN"
    }
}
