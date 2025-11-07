package ai.accelera.library.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup

/**
 * Extension to get parent Activity from View (similar to parentViewController in iOS).
 */
val View.parentActivity: Activity?
    get() {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

/**
 * Extension to get parent Activity from ViewGroup.
 */
val ViewGroup.parentActivity: Activity?
    get() {
        var context = context
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

