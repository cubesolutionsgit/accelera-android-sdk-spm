package ai.accelera.library.banners.infrastructure.divkit

import android.content.Context
import android.graphics.Typeface
import androidx.core.graphics.TypefaceCompat
import ai.accelera.library.Accelera
import ai.accelera.library.AcceleraFontProvider
import com.yandex.div.core.font.DivTypefaceProvider

/** One cache per content view; no Activity or fonts retained in process-wide registries. */
internal class AcceleraFontResolver(
    context: Context,
    private val provider: AcceleraFontProvider? = Accelera.shared.getDelegate()?.fontProvider,
) {
    private val context = context.applicationContext ?: context
    private data class Key(val family: String, val weight: Int, val italic: Boolean, val faceName: String?)
    private val fonts = mutableMapOf<Key, Typeface>()
    private val assetNames = runCatching { this.context.assets.list("fonts")?.toSet().orEmpty() }
        .getOrDefault(emptySet())

    fun resolve(family: String, weight: Int = 400, italic: Boolean = false, faceName: String? = null): Typeface {
        val key = Key(family, if (weight > 0) weight.coerceIn(100, 900) else 400, italic, faceName)
        return fonts.getOrPut(key) {
            provider?.let {
                runCatching {
                    faceName?.takeIf { name -> name.isNotBlank() && name != family }
                        ?.let { name -> it.getTypeface(name, key.weight, key.italic) }
                        ?: it.getTypeface(key.family, key.weight, key.italic)
                }
                    .onFailure { error -> Accelera.shared.error("Font provider failed for $family: ${error.message}") }
                    .getOrNull()
            } ?: fromAssets(key) ?: TypefaceCompat.create(
                context, Typeface.create(family, Typeface.NORMAL), key.weight, italic
            )
        }
    }

    private fun fromAssets(key: Key): Typeface? {
        val weightName = when (key.weight) {
            100 -> "Thin"
            200 -> "ExtraLight"
            300 -> "Light"
            400 -> "Regular"
            500 -> "Medium"
            600 -> "SemiBold"
            700 -> "Bold"
            800 -> "ExtraBold"
            900 -> "Black"
            else -> null
        }
        val style = if (key.italic) {
            if (key.weight == 400) "Italic" else weightName?.plus("Italic")
        } else weightName
        val candidates = buildList {
            key.faceName?.takeIf { it.isNotBlank() && it != key.family }?.let { add(it) }
            if (style != null) add("${key.family}-$style")
            add(key.family)
            if (style != "Regular") add("${key.family}-Regular")
        }
        for (name in candidates) for (extension in listOf("ttf", "otf")) {
            val file = "$name.$extension"
            if (file !in assetNames) continue
            val face = runCatching { Typeface.createFromAsset(context.assets, "fonts/$file") }
                .onFailure { Accelera.shared.error("Failed to load font $file: ${it.message}") }
                .getOrNull() ?: continue
            // A specific asset face already carries its style. A family base file may
            // be variable, so preserve the requested numeric weight when adapting it.
            val exactFace = name == key.faceName && name != key.family
            val matchingStyle = style != null && name == "${key.family}-$style"
            val namedFace = name == key.family && key.weight == 400 && !key.italic &&
                key.family.substringAfterLast('-') in setOf(
                    "Thin", "ExtraLight", "Light", "Regular", "Medium", "SemiBold",
                    "Bold", "ExtraBold", "Black", "Italic", "BoldItalic", "SemiBoldItalic"
                )
            return if (exactFace || matchingStyle || namedFace) face
                else TypefaceCompat.create(context, face, key.weight, key.italic)
        }
        return null
    }

    fun divProvider(family: String): DivTypefaceProvider = object : DivTypefaceProvider {
        override fun getRegular(): Typeface = resolve(family, 400)
        override fun getMedium(): Typeface = resolve(family, 500)
        override fun getLight(): Typeface = resolve(family, 300)
        override fun getBold(): Typeface = resolve(family, 700)
        override fun getTypefaceFor(weight: Int): Typeface = resolve(family, weight)
    }

    // DivKit looks providers up by the evaluated font_family, which can be an expression.
    fun divProviders(): Map<String, DivTypefaceProvider> = object : HashMap<String, DivTypefaceProvider>() {
        override fun get(key: String): DivTypefaceProvider = super.get(key)
            ?: divProvider(key).also { put(key, it) }
    }
}
