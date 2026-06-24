package com.metro.navbar

import android.accessibilityservice.AccessibilityService
import java.util.concurrent.atomic.AtomicReference

class NavbarAccessibilityService : AccessibilityService() {
  override fun onServiceConnected() {
    instance.set(this)
    NavbarOverlayController.onAccessibilityServiceConnected(this)
  }

  override fun onDestroy() {
    NavbarOverlayController.onAccessibilityServiceDisconnected()
    instance.compareAndSet(this, null)
    super.onDestroy()
  }

  override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) = Unit

  override fun onInterrupt() = Unit

  companion object {
    private val instance = AtomicReference<NavbarAccessibilityService?>()

    fun getInstance(): NavbarAccessibilityService? = instance.get()

    fun performBack(): Boolean {
      val service = instance.get() ?: return false
      return service.performGlobalAction(GLOBAL_ACTION_BACK)
    }

    fun performRecents(): Boolean {
      val service = instance.get() ?: return false
      return service.performGlobalAction(GLOBAL_ACTION_RECENTS)
    }

    fun isEnabled(): Boolean = instance.get() != null
  }
}
