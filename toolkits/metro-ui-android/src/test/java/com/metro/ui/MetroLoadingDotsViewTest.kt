package com.metro.ui

import android.view.View
import android.widget.FrameLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class MetroLoadingDotsViewTest {

    @Test
    fun start_animatesFirstDotIntoTrack() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val host = FrameLayout(context)
        val dots = MetroLoadingDotsView(context)
        host.addView(dots)
        attach(host)
        layout(host, dots)

        dots.start()
        ShadowLooper.idleMainLooper(200, java.util.concurrent.TimeUnit.MILLISECONDS)

        val firstDot = dots.getChildAt(0)
        assertTrue(
            "Dot should move into the visible track (translationX > -10px)",
            firstDot.translationX > -10f,
        )
        assertNotEquals(0f, dots.translationX)
    }

    @Test
    fun onStarted_firesWhenStartRuns() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val host = FrameLayout(context)
        val dots = MetroLoadingDotsView(context)
        var started = false
        dots.onStarted = { started = true }
        host.addView(dots)
        attach(host)
        layout(host, dots)

        dots.start()
        assertTrue(started)
    }

    private fun attach(host: FrameLayout) {
        val activity = org.robolectric.Robolectric.buildActivity(
            android.app.Activity::class.java,
        ).create().start().resume().get()
        activity.setContentView(host)
        ShadowLooper.idleMainLooper()
    }

    private fun layout(host: FrameLayout, dots: MetroLoadingDotsView) {
        host.measure(
            View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY),
        )
        host.layout(0, 0, 1080, 1920)
        dots.ensureStarted()
        ShadowLooper.idleMainLooper()
    }
}
