package ai.accelera.library.banners.infrastructure.divkit

import android.graphics.Typeface
import android.view.View
import com.airbnb.lottie.FontAssetDelegate
import com.airbnb.lottie.LottieDrawable
import com.yandex.div.core.view2.Div2View
import com.yandex.div.core.widget.LoadableImageView
import com.yandex.div.json.expressions.ExpressionResolver
import com.yandex.div.lottie.DivLottieExtensionHandler
import com.yandex.div.lottie.DivLottieLogger
import com.yandex.div.lottie.DivLottieRawResProvider
import com.yandex.div2.DivBase

/** Remote animations may reference fonts that the consuming application does not bundle. */
internal class AcceleraLottieExtensionHandler(
    rawResProvider: DivLottieRawResProvider,
    logger: DivLottieLogger,
    private val fonts: AcceleraFontResolver,
) : DivLottieExtensionHandler(rawResProvider, logger) {
    override fun beforeBindView(
        divView: Div2View,
        expressionResolver: ExpressionResolver,
        view: View,
        div: DivBase,
    ) {
        super.beforeBindView(divView, expressionResolver, view, div)
        val image = view as? LoadableImageView ?: return
        val fontDelegate = AcceleraLottieFonts(fonts)
        // DivKit assigns the drawable asynchronously. Install the delegate on assignment,
        // before the first draw, including when the view is rebound to another animation.
        image.setImageChangeCallback {
            (image.drawable as? LottieDrawable)?.let { configureFonts(it, fontDelegate) }
        }
        (image.drawable as? LottieDrawable)?.let { configureFonts(it, fontDelegate) }
    }

    internal fun configureFonts(drawable: LottieDrawable, delegate: AcceleraLottieFonts) {
        drawable.setFontAssetDelegate(delegate)
        val composition = drawable.composition
        // Lottie caches a single base face per family in FontAssetManager. A map by
        // fName retains different weights/faces in the same animation. Do not set a
        // map for glyph-based animations: Lottie would stop using embedded outlines.
        drawable.setFontMap(if (composition != null && composition.characters.size() == 0) {
            val families = composition.fonts.values.map { it.family }.toSet()
            buildMap {
                for (font in composition.fonts.values) {
                    val face = font.typeface ?: delegate.fetchFont(font.family, font.style, font.name)
                    put("${font.family}-${font.style}", face)
                    // Lottie checks family keys before face keys. A face named like
                    // its family must not override every other weight of that family.
                    if (font.name !in families) put(font.name, face)
                }
            }
        } else null)
    }

    override fun unbindView(
        divView: Div2View,
        expressionResolver: ExpressionResolver,
        view: View,
        div: DivBase,
    ) {
        super.unbindView(divView, expressionResolver, view, div)
        (view as? LoadableImageView)?.setImageChangeCallback(null)
    }
}

internal class AcceleraLottieFonts(private val fonts: AcceleraFontResolver) : FontAssetDelegate() {
    override fun fetchFont(fontFamily: String): Typeface = fonts.resolve(fontFamily)

    override fun fetchFont(fontFamily: String, fontStyle: String, fontName: String): Typeface {
        val weight = when {
            fontStyle.contains("Black", true) || fontStyle.contains("Heavy", true) -> 900
            fontStyle.contains("ExtraBold", true) || fontStyle.contains("Extra Bold", true) -> 800
            fontStyle.contains("SemiBold", true) || fontStyle.contains("Semi Bold", true) -> 600
            fontStyle.contains("Bold", true) -> 700
            fontStyle.contains("Medium", true) -> 500
            fontStyle.contains("ExtraLight", true) || fontStyle.contains("Extra Light", true) -> 200
            fontStyle.contains("Light", true) -> 300
            fontStyle.contains("Thin", true) -> 100
            else -> 400
        }
        return fonts.resolve(fontFamily, weight, fontStyle.contains("Italic", true), fontName)
    }
}
