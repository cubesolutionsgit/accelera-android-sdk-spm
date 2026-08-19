package ai.accelera.library.banners.presentation.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FullscreenActivitySmokeTest {
    @Test
    fun launch_withRequiredExtras_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val json = """
            {
              "fullscreens": {
                "entry-1": {
                  "cards": [
                    {
                      "card": {
                        "log_id": "fullscreen_smoke",
                        "states": [
                          {
                            "state_id": 0,
                            "div": { "type": "text", "text": "Smoke test" }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent().toByteArray()
        val intent = Intent(context, FullscreenActivity::class.java).apply {
            putExtra(FullscreenActivity.EXTRA_JSON_DATA, json)
            putExtra(FullscreenActivity.EXTRA_ENTRY_ID, "entry-1")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<FullscreenActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
