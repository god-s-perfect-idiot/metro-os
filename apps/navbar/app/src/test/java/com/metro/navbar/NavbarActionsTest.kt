package com.metro.navbar

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class NavbarActionsTest {
  @Test
  fun launchGoogleSearch_prefersGoogleGlobalSearchIntent() {
    val context = RuntimeEnvironment.getApplication()
    val searchIntent = Intent("android.search.action.GLOBAL_SEARCH").apply {
      setPackage(NavbarSpec.GOOGLE_SEARCH_PACKAGE)
    }
    shadowOf(context.packageManager).addResolveInfoForIntent(
      searchIntent,
      resolveInfo(NavbarSpec.GOOGLE_SEARCH_PACKAGE, "SearchActivity"),
    )

    NavbarActions.launchGoogleSearch(context)

    val started = shadowOf(context).nextStartedActivity
    assertEquals("android.search.action.GLOBAL_SEARCH", started.action)
    assertEquals(NavbarSpec.GOOGLE_SEARCH_PACKAGE, started.`package`)
    assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
  }

  @Test
  fun launchGemini_targetsBardEntryPoint() {
    val context = RuntimeEnvironment.getApplication()
    NavbarActions.launchGemini(context)

    val started = shadowOf(context).nextStartedActivity
    assertNotNull(started)
    assertEquals(NavbarSpec.GEMINI_PACKAGE, started.component?.packageName)
    assertEquals(NavbarSpec.GEMINI_ENTRY_ACTIVITY, started.component?.className)
  }

  @Test
  fun launchStart_fallsBackToHomeIntentWhenLauncherMissing() {
    val context = RuntimeEnvironment.getApplication()
    NavbarActions.launchStart(context)

    val started = shadowOf(context).nextStartedActivity
    assertEquals(Intent.ACTION_MAIN, started.action)
    assertTrue(started.categories?.contains(Intent.CATEGORY_HOME) == true)
    assertTrue(started.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
  }

  private fun resolveInfo(packageName: String, className: String): ResolveInfo =
    ResolveInfo().apply {
      activityInfo = ActivityInfo().apply {
        this.packageName = packageName
        name = className
      }
    }
}
