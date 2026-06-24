package com.metro.navbar

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object NavbarActions {
  fun launchStart(context: Context) {
    val launchIntent = context.packageManager.getLaunchIntentForPackage(NavbarSpec.LAUNCHER_PACKAGE)
      ?: Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_HOME)
      }
    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    context.startActivity(launchIntent)
  }

  fun launchGoogleSearch(context: Context) {
    val googleSearch = Intent("android.search.action.GLOBAL_SEARCH").apply {
      setPackage(NavbarSpec.GOOGLE_SEARCH_PACKAGE)
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (startIfResolvable(context, googleSearch)) {
      return
    }

    val webSearch = Intent(Intent.ACTION_WEB_SEARCH).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    if (startIfResolvable(context, webSearch)) {
      return
    }

    startIfResolvable(
      context,
      Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      },
    )
  }

  fun launchGemini(context: Context) {
    val geminiLaunch = context.packageManager.getLaunchIntentForPackage(NavbarSpec.GEMINI_PACKAGE)
      ?: Intent(Intent.ACTION_MAIN).apply {
        setClassName(NavbarSpec.GEMINI_PACKAGE, NavbarSpec.GEMINI_ENTRY_ACTIVITY)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
    if (startIfResolvable(context, geminiLaunch)) {
      return
    }

    val assist = Intent(Intent.ACTION_ASSIST).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    startIfResolvable(context, assist)
  }

  fun dispatchBack(context: Context): Boolean {
    if (NavbarAccessibilityService.performBack()) {
      return true
    }
    context.sendBroadcast(
      Intent(ACTION_NAV_BACK).apply {
        setPackage(context.packageName)
      },
    )
    return false
  }

  fun dispatchRecents(context: Context): Boolean =
    NavbarAccessibilityService.performRecents()

  fun openAccessibilitySettings(context: Context) {
    context.startActivity(
      Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      },
    )
  }

  private fun startIfResolvable(context: Context, intent: Intent): Boolean {
    if (intent.resolveActivity(context.packageManager) == null) {
      return false
    }
    return try {
      context.startActivity(intent)
      true
    } catch (_: ActivityNotFoundException) {
      false
    }
  }

  const val ACTION_NAV_BACK = "com.metro.navbar.action.NAV_BACK"
}
