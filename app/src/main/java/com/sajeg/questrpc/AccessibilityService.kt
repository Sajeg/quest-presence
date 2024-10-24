package com.sajeg.questrpc

import android.accessibilityservice.AccessibilityService
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.my.kizzyrpc.KizzyRPC

class AccessibilityService : AccessibilityService() {
    var rpc: KizzyRPC? = null
    var lastPackage = ""

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {
        if (p0 != null && p0.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = p0.packageName?.toString()
            if (lastPackage == packageName) {
                return
            }
            lastPackage = packageName.toString()
            AppManager().getExcludedApps(this) { apps ->
                if (apps.contains(packageName)) {
                    return@getExcludedApps
                }
                SettingsManager().readString("token", this) { token ->
                    rpc = KizzyRPC(token)
                    val appName = getAppNameFromPackageName(packageName.toString())
                    createActivity(rpc!!, appName)
                }
            }
        }
    }

    override fun onInterrupt() {
        if (rpc == null) {
            return
        }
        rpc!!.closeRPC()
    }

    private fun getAppNameFromPackageName(packageName: String): String {
        return when (packageName) {
            "com.oculus.systemux" -> "Online"
            else -> {
                try {
                    val packageManager = packageManager
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    packageManager.getApplicationLabel(appInfo).toString()
                } catch (e: PackageManager.NameNotFoundException) {
                    packageName
                }
            }
        }
    }
}