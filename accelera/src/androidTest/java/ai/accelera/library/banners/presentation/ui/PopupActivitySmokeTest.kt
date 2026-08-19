package ai.accelera.library.banners.presentation.ui

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PopupActivitySmokeTest {
    @Test
    fun launch_withoutPayload_finishesWithoutCrash() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(context, PopupActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        ActivityScenario.launch<PopupActivity>(intent).use { scenario ->
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.DESTROYED)
        }
    }
}
