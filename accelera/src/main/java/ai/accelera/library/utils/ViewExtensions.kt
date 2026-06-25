package ai.accelera.library.utils

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import android.view.ViewGroup

/**
 * Extension to unwrap ContextWrapper layers and find an Activity.
 */
val Context.parentActivity: Activity?
    get() {
        var context = this
        while (context is ContextWrapper) {
            if (context is Activity) {
                return context
            }
            context = context.baseContext
        }
        return null
    }

/**
 * Extension to get parent Activity from View (similar to parentViewController in iOS).
 */
val View.parentActivity: Activity?
    get() = context.parentActivity

/**
 * Extension to get parent Activity from ViewGroup.
 */
val ViewGroup.parentActivity: Activity?
    get() = context.parentActivity

/**
 * Converts a dp value to integer pixels using the current display density.
 */
fun Context.dpToPx(dp: Number): Int = (dp.toFloat() * resources.displayMetrics.density).toInt()

/**
 * Converts a dp value to float pixels using the current display density.
 */
fun Context.dpToPxF(dp: Number): Float = dp.toFloat() * resources.displayMetrics.density

/**
 * Converts a dp value to integer pixels using the view's display density.
 */
fun View.dpToPx(dp: Number): Int = context.dpToPx(dp)

/**
 * Converts a dp value to float pixels using the view's display density.
 */
fun View.dpToPxF(dp: Number): Float = context.dpToPxF(dp)
