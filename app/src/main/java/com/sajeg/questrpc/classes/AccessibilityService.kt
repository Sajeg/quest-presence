package com.sajeg.questrpc.classes

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
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
            Log.d("ActivityManagerPackage", packageName.toString())
            Log.d("ActivityManagerClass", p0.className.toString())
            if (packageName == "com.oculus.shellenv" && p0.className.toString() == "com.oculus.shellenv.ShellEnvActivity") {
                ActivityManager.stop(this)
            }
            ActivityManager.appChanged(packageName.toString(), this)
        }
    }

    override fun onInterrupt() {
    }
}