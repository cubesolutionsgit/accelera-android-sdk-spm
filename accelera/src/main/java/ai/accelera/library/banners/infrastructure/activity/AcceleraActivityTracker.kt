package ai.accelera.library.banners.infrastructure.activity

import android.app.Activity
import android.app.Application
import android.content.Context
import android.os.Bundle
import java.lang.ref.WeakReference

internal object AcceleraActivityTracker {
    private var currentActivityRef: WeakReference<Activity>? = null
    private var registeredApplicationRef: WeakReference<Application>? = null

    fun register(context: Context) {
        val application = context.applicationContext as? Application ?: return
        if (registeredApplicationRef?.get() === application) return

        application.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityStarted(activity: Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityResumed(activity: Activity) {
                currentActivityRef = WeakReference(activity)
            }

            override fun onActivityPaused(activity: Activity) = Unit
            override fun onActivityStopped(activity: Activity) = Unit
            override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

            override fun onActivityDestroyed(activity: Activity) {
                if (currentActivityRef?.get() === activity) {
                    currentActivityRef = null
                }
            }
        })
        registeredApplicationRef = WeakReference(application)
    }

    fun note(activity: Activity) {
        currentActivityRef = WeakReference(activity)
        register(activity)
    }

    fun currentActivity(): Activity? = currentActivityRef?.get()
}
