package com.metro.navbar

import android.provider.Settings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class SystemNavigationModeTest {
  @Test
  @Config(sdk = [29])
  fun isThreeButton_whenSecureSettingIsZero() {
    val context = RuntimeEnvironment.getApplication()
    Settings.Secure.putInt(context.contentResolver, "navigation_mode", 0)

    assertEquals(SystemNavigationMode.THREE_BUTTON, SystemNavigationMode.mode(context))
    assertTrue(SystemNavigationMode.isThreeButton(context))
  }

  @Test
  @Config(sdk = [29])
  fun isThreeButton_falseWhenGestureNavigation() {
    val context = RuntimeEnvironment.getApplication()
    Settings.Secure.putInt(context.contentResolver, "navigation_mode", 2)

    assertEquals(SystemNavigationMode.GESTURE, SystemNavigationMode.mode(context))
    assertFalse(SystemNavigationMode.isThreeButton(context))
  }

  @Test
  @Config(sdk = [29])
  fun isThreeButton_falseWhenTwoButtonNavigation() {
    val context = RuntimeEnvironment.getApplication()
    Settings.Secure.putInt(context.contentResolver, "navigation_mode", 1)

    assertEquals(SystemNavigationMode.TWO_BUTTON, SystemNavigationMode.mode(context))
    assertFalse(SystemNavigationMode.isThreeButton(context))
  }

  @Test
  @Config(sdk = [28])
  fun mode_defaultsToThreeButtonOnPreQWithoutResource() {
    val context = RuntimeEnvironment.getApplication()
    assertEquals(SystemNavigationMode.THREE_BUTTON, SystemNavigationMode.mode(context))
    assertTrue(SystemNavigationMode.isThreeButton(context))
  }
}
