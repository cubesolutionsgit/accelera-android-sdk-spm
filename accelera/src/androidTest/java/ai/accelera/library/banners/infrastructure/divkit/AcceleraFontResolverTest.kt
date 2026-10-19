package ai.accelera.library.banners.infrastructure.divkit

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.res.AssetManager
import android.graphics.Typeface
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.TypefaceCompat
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ai.accelera.library.AcceleraFontProvider
import ai.accelera.library.Accelera
import ai.accelera.library.AcceleraDelegate
import ai.accelera.library.DefaultAcceleraDelegate
import ai.accelera.library.banners.infrastructure.cache.AcceleraPayloadRegistry
import ai.accelera.library.banners.presentation.ui.PopupActivity
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.LottieDrawable
import com.yandex.div.lottie.DivLottieLogger
import com.yandex.div.lottie.DivLottieRawResProvider
import org.junit.Assert.*
import org.junit.Test

class AcceleraFontResolverTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = object : ContextWrapper(instrumentation.targetContext) {
        override fun getApplicationContext(): Context = this
        override fun getAssets(): AssetManager = instrumentation.context.assets
    }

    @Test fun bundledFamilyIsUsedAndCached() {
        val resolver = AcceleraFontResolver(context, null)
        val actual = resolver.resolve("Teko")
        assertEquals(tekoRegular(), actual)
        assertSame(actual, resolver.resolve("Teko"))
        assertNotEquals(Typeface.DEFAULT, actual)
    }

    @Test fun missingFamilyFallsBackWithRequestedWeight() {
        val resolver = AcceleraFontResolver(context, null)
        val actual = resolver.resolve("nonexistent-font-for-accelera", 700, true)
        assertTrue(actual.isBold)
        assertTrue(actual.isItalic)
    }

    @Test fun otfFamilySupportsExplicitFaceAndRegularFallback() {
        val resolver = AcceleraFontResolver(context, null)
        val expected = Typeface.createFromAsset(context.assets, "fonts/SourceSans3-Regular.otf")
        assertEquals(expected, resolver.resolve("SourceSans3"))
        assertEquals(expected, resolver.resolve("SourceSans3-Regular"))
        val bold = resolver.resolve("SourceSans3", 700)
        assertTrue(bold.isBold)
        if (Build.VERSION.SDK_INT >= 28) assertEquals(700, bold.weight)
    }

    @Test fun customProviderReceivesNumericWeightsAndWinsOverAssets() {
        val calls = mutableListOf<Triple<String, Int, Boolean>>()
        val resolver = AcceleraFontResolver(context, AcceleraFontProvider { family, weight, italic ->
            calls.add(Triple(family, weight, italic))
            Typeface.MONOSPACE
        })
        assertSame(Typeface.MONOSPACE, resolver.divProviders()["Teko"]!!.getTypefaceFor(650))
        assertSame(Typeface.MONOSPACE, resolver.resolve("Teko", 650))
        assertEquals(listOf(Triple("Teko", 650, false)), calls)
    }

    @Test fun providerFailureAndNullResultFallBackToBundledFont() {
        for (provider in listOf(
            AcceleraFontProvider { _, _, _ -> null },
            AcceleraFontProvider { _, _, _ -> error("Host font loading failed") }
        )) {
            val actual = AcceleraFontResolver(context, provider).resolve("Teko")
            assertEquals(tekoRegular(), actual)
        }
    }

    @Test fun variableFamilyKeepsNumericWeightOnSupportedAndroid() {
        val resolver = AcceleraFontResolver(context, null)
        val actual = resolver.divProvider("Teko").getTypefaceFor(550)!!
        if (Build.VERSION.SDK_INT >= 28) assertEquals(550, actual.weight)
    }

    @Test fun lottieHonorsExplicitFaceAndStyle() {
        val calls = mutableListOf<Triple<String, Int, Boolean>>()
        val resolver = AcceleraFontResolver(context, AcceleraFontProvider { name, weight, italic ->
            calls.add(Triple(name, weight, italic))
            if (name == "Brand-SemiBoldItalic") Typeface.SERIF else null
        })
        val lottie = AcceleraLottieFonts(resolver)
        assertSame(Typeface.SERIF, lottie.fetchFont("Brand", "SemiBold Italic", "Brand-SemiBoldItalic"))
        assertEquals(listOf(Triple("Brand-SemiBoldItalic", 600, true)), calls)
    }

    @Test fun divKitUsesProviderForExpressionFamilyAndNumericWeight() {
        val previous = Accelera.shared.getDelegate()
        val calls = mutableListOf<Triple<String, Int, Boolean>>()
        Accelera.shared.setDelegate(object : AcceleraDelegate by (previous ?: DefaultAcceleraDelegate()) {
            override val fontProvider = AcceleraFontProvider { family, weight, italic ->
                calls.add(Triple(family, weight, italic))
                Typeface.MONOSPACE
            }
        })
        val payload = """{"card":{"log_id":"font_test",
          "variables":[{"name":"family","type":"string","value":"Brand"}],
          "states":[{"state_id":0,"div":{"type":"text","text":"font probe",
          "font_family":"@{family}","font_weight_value":650,
          "height":{"type":"wrap_content"}}}]}}""".toByteArray()
        val token = AcceleraPayloadRegistry.register(payload)
        try {
            val intent = Intent(instrumentation.targetContext, PopupActivity::class.java)
                .putExtra(PopupActivity.EXTRA_PAYLOAD_TOKEN, token)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            ActivityScenario.launch<PopupActivity>(intent).use { scenario ->
                scenario.onActivity {
                    val text = findText(it.window.decorView)
                    assertNotNull("DivKit must render the text element", text)
                    assertEquals(Typeface.MONOSPACE, text!!.typeface)
                    assertTrue(calls.contains(Triple("Brand", 650, false)))
                }
            }
        } finally {
            Accelera.shared.setDelegate(previous)
            AcceleraPayloadRegistry.remove(token)
        }
    }

    @Test fun lottieKeepsMultipleFacesOfSameFamily() {
        val resolver = AcceleraFontResolver(context, AcceleraFontProvider { _, weight, _ ->
            if (weight == 700) Typeface.MONOSPACE else Typeface.SERIF
        })
        val composition = LottieCompositionFactory.fromJsonStringSync("""{
            "v":"5.7.4","w":100,"h":100,"ip":0,"op":60,"fr":30,"layers":[],
            "fonts":{"list":[
              {"fFamily":"Brand","fName":"Brand","fStyle":"Regular","ascent":75},
              {"fFamily":"Brand","fName":"Brand-Bold","fStyle":"Bold","ascent":75}
            ]}}""", null).value!!
        val drawable = LottieDrawable()
        drawable.setComposition(composition)
        AcceleraLottieExtensionHandler(DivLottieRawResProvider.STUB, DivLottieLogger.STUB, resolver)
            .configureFonts(drawable, AcceleraLottieFonts(resolver))
        assertSame(Typeface.SERIF, drawable.getTypeface(composition.fonts.getValue("Brand")))
        assertSame(Typeface.MONOSPACE, drawable.getTypeface(composition.fonts.getValue("Brand-Bold")))
    }

    @Test fun embeddedGlyphsRemainEnabled() {
        val resolver = AcceleraFontResolver(context, AcceleraFontProvider { _, _, _ ->
            error("Glyph animation should not need a host font")
        })
        val composition = LottieCompositionFactory.fromJsonStringSync("""{
            "v":"5.7.4","w":100,"h":100,"ip":0,"op":60,"fr":30,"layers":[],
            "fonts":{"list":[{"fFamily":"Brand","fName":"Brand","fStyle":"Regular","ascent":75}]},
            "chars":[{"ch":"A","size":20,"style":"Regular","w":20,"fFamily":"Brand",
              "data":{"shapes":[]}}]}""", null).value!!
        val drawable = LottieDrawable()
        drawable.setComposition(composition)
        AcceleraLottieExtensionHandler(DivLottieRawResProvider.STUB, DivLottieLogger.STUB, resolver)
            .configureFonts(drawable, AcceleraLottieFonts(resolver))
        assertTrue(drawable.useTextGlyphs())
    }

    private fun findText(view: View): TextView? {
        if (view is TextView && view.text.toString() == "font probe") return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            findText(view.getChildAt(index))?.let { return it }
        }
        return null
    }

    private fun tekoRegular(): Typeface = TypefaceCompat.create(
        context, Typeface.createFromAsset(context.assets, "fonts/Teko.ttf"), 400, false
    )
}
