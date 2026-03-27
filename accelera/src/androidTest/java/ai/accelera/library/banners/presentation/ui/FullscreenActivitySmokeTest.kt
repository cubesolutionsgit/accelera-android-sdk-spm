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
        val intent = Intent(context, FullscreenActivity::class.java).apply {
            putExtra("jsonData", """{"fullscreens":{"entry-1":{"cards":[]}}}""".toByteArray())
            putExtra("entryId", "entry-1")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<FullscreenActivity>(intent).use { scenario ->
            scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }
        }
    }
}
