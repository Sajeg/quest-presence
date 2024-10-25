package com.sajeg.questrpc

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.accessibility.AccessibilityEvent
import com.my.kizzyrpc.KizzyRPC

class AccessibilityService : AccessibilityService() {
    var rpc: KizzyRPC? = null
    var lastPackage = ""

    override fun onServiceConnected() {
        super.onServiceConnected()
        SettingsManager().saveString("service", "active", this)
    }

    override fun onDestroy() {
        super.onDestroy()
        SettingsManager().saveString("service", "closed", this)
        if (rpc != null) {
            rpc!!.closeRPC()
        }
    }

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
                    if (packageName == "com.oculus.vrshell") {
                        createActivity(rpc!!, "Online on Quest")
                    }
                    AppManager().getCustomAppNames(this) { names ->
                        names.forEach { name ->
                            if (name.packageName == packageName) {
                                createActivity(rpc!!, name.name)
                                return@getCustomAppNames
                            }
                        }
                        val appName = getAppNameFromPackageName(packageName.toString())
                        if (appName == "Invalid") {
                            return@getCustomAppNames
                        }
                        createActivity(rpc!!, appName)
                    }
                }
            }
        }
    }

    override fun onInterrupt() {
    }

    private fun getAppNameFromPackageName(packageName: String): String {
        return try {
            val packageManager = packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)

            if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                packageManager.getApplicationLabel(appInfo).toString()
            } else {
                "Invalid"
            }

        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }
}