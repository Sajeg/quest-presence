package com.sajeg.questrpc

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent

class AccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()

        ActivityManager.start(this)

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        registerReceiver(ScreenStateReceiver(), filter)

    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(ScreenStateReceiver())
        ActivityManager.stop(this)
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        if (p0 != null && p0.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = p0.packageName?.toString()
            ActivityManager.appChanged(packageName.toString(), this)
        }
    }

    override fun onInterrupt() {
    }
}