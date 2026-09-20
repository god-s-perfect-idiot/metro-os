package com.metro.hub

import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.metro.hub.data.HubLogoDecoder
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HubLogoDecoderInstrumentedTest {
    @Test
    fun decodesInlineVectorXmlToBitmap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Mirrors second-party Metro Notes logo shape from sync-hub-firestore.sh.
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="200dp"
                android:height="200dp"
                android:viewportWidth="512"
                android:viewportHeight="512">
                <path
                    android:fillColor="#FFFFFF"
                    android:pathData="M320 0v128h128L320 0zm-21.3 0H64v512h384V149.3H298.7V0z"/>
            </vector>
        """.trimIndent()
        val bitmap = HubLogoDecoder.bitmapFromLogoXml(context, xml, sizePx = 96)
        assertNotNull("vector logoXml must rasterize on device", bitmap)
        assertTrue(bitmap!!.width == 96 && bitmap.height == 96)
        assertTrue("expected opaque glyph pixels", countOpaque(bitmap) > 100)
    }

    @Test
    fun remoteUrlDoesNotRasterizeAsVector() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertTrue(
            HubLogoDecoder.bitmapFromLogoXml(
                context,
                "https://cdn.example.com/logo.png",
            ) == null,
        )
    }

    @Test
    fun respectsEvenOddFillTypeAndGroupScale() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Mirrors metro_app_photos.xml — evenOdd frame + 0.70 group scale.
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp"
                android:height="108dp"
                android:viewportWidth="108"
                android:viewportHeight="108">
                <group
                    android:pivotX="54"
                    android:pivotY="54"
                    android:scaleX="0.70"
                    android:scaleY="0.70">
                    <path
                        android:fillColor="#FFFFFF"
                        android:fillType="evenOdd"
                        android:pathData="M23,23h61v61h-61zM27,27h53v53h-53z" />
                </group>
            </vector>
        """.trimIndent()
        val size = 108
        val bitmap = HubLogoDecoder.bitmapFromLogoXml(context, xml, sizePx = size)
        assertNotNull(bitmap)
        // Interior of the hollow frame (viewport center) must stay transparent.
        assertTrue(
            "evenOdd hole must be transparent",
            Color.alpha(bitmap!!.getPixel(size / 2, size / 2)) == 0,
        )
        // Without 0.70 group scale the outer rim sits at y≈23–27; with scale it moves to y≈32–35.
        assertTrue(
            "unscaled rim location must be empty after group scale",
            Color.alpha(bitmap.getPixel(size / 2, 25)) == 0,
        )
        assertTrue(
            "scaled frame rim must be opaque",
            Color.alpha(bitmap.getPixel(size / 2, 33)) > 200,
        )
    }

    @Test
    fun respectsFillAlphaZeroWithStroke() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Mirrors metro_app_notes outline — transparent fill + white stroke.
        val xml = """
            <?xml version="1.0" encoding="utf-8"?>
            <vector xmlns:android="http://schemas.android.com/apk/res/android"
                android:width="108dp"
                android:height="108dp"
                android:viewportWidth="108"
                android:viewportHeight="108">
                <path
                    android:fillColor="@android:color/transparent"
                    android:pathData="M34,28h40v52h-40z"
                    android:strokeColor="#FFFFFF"
                    android:strokeWidth="5" />
            </vector>
        """.trimIndent()
        val size = 108
        val bitmap = HubLogoDecoder.bitmapFromLogoXml(context, xml, sizePx = size)!!
        // Center of the stroked rectangle must be hollow.
        assertTrue(
            "stroked rect interior must be transparent",
            Color.alpha(bitmap.getPixel(size / 2, size / 2)) == 0,
        )
        assertTrue("stroke must paint opaque pixels", countOpaque(bitmap) > 50)
    }

    private fun countOpaque(bitmap: android.graphics.Bitmap): Int {
        var count = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                if (Color.alpha(bitmap.getPixel(x, y)) > 200) count++
            }
        }
        return count
    }
}
