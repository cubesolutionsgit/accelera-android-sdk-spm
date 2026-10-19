package ai.accelera.library

import android.graphics.Typeface

/**
 * Resolves host application fonts for DivKit text and Lottie animations.
 * Install through [AcceleraDelegate.fontProvider] before attaching content.
 * Return null to use assets/fonts or a system font. Weight is in 100..900.
 * An explicit face name (for example Brand-Bold) should return that exact face.
 */
fun interface AcceleraFontProvider {
    fun getTypeface(family: String, weight: Int, italic: Boolean): Typeface?
}
