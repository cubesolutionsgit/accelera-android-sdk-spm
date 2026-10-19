package ai.accelera.library.banners.infrastructure.divkit

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import ai.accelera.library.banners.infrastructure.cache.AcceleraPayloadRegistry
import ai.accelera.library.banners.presentation.ui.PopupActivity
import com.airbnb.lottie.LottieDrawable
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class LottieTemplateTest {
    private val fixture = """{
      "templates":{"animated":{"type":"gif","gif_url":"empty://","extensions":[
        {"id":"lottie","${'$'}params":"animation"}]}},
      "card":{"log_id":"lottie_test","states":[{"state_id":0,"div":{
        "type":"animated","width":{"type":"fixed","value":120},
        "height":{"type":"fixed","value":120},"animation":{
          "lottie_url":"https://assets.lottiefiles.com/packages/lf20_UJNc2t.json",
          "repeat_count":-1,"repeat_mode":"restart"}}}]}}
    """.trimIndent().toByteArray()

    @Test fun sharedTemplatesAreAvailableToExtractedStoryCards() {
        val root = JSONObject(String(fixture))
        val card = root.getJSONObject("card").toString().toByteArray()
        val data = DivKitSetup.parseDivData(card, fixture)
        assertNotNull(data)
        assertEquals("lottie_test", data!!.logId)
        assertTrue(data.states.isNotEmpty())
    }

    @Test fun templateAnimationLoadsAndAdvancesFrames() {
        val payload = fixture
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val token = AcceleraPayloadRegistry.register(payload)
        val intent = Intent(context, PopupActivity::class.java)
            .putExtra(PopupActivity.EXTRA_PAYLOAD_TOKEN, token)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            ActivityScenario.launch<PopupActivity>(intent).use { scenario ->
                var initialFrame: Float? = null
                var advanced = false
                val deadline = android.os.SystemClock.uptimeMillis() + 30000
                while (!advanced && android.os.SystemClock.uptimeMillis() < deadline) {
                    scenario.onActivity { activity ->
                        val animation = findAnimation(activity.window.decorView)
                        if (animation?.composition != null && animation.isAnimating) {
                            assertEquals(-1, animation.repeatCount)
                            val first = initialFrame
                            if (first == null) initialFrame = animation.frame.toFloat()
                            else advanced = first != animation.frame.toFloat()
                        }
                    }
                    if (!advanced) android.os.SystemClock.sleep(100)
                }
                assertTrue("Lottie must load from the template URL and advance frames", advanced)
            }
        } finally {
            AcceleraPayloadRegistry.remove(token)
        }
    }

    private fun findAnimation(view: View): LottieDrawable? {
        if (view is ImageView && view.drawable is LottieDrawable)
            return view.drawable as LottieDrawable
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findAnimation(view.getChildAt(index))?.let { return it }
        }
        return null
    }
}
