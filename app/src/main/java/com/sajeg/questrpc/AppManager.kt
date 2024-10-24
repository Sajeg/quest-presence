package com.sajeg.questrpc

import android.content.Context

class AppManager {
    fun getExcludedApps(context: Context, onResponse: (apps: List<String>) -> Unit) {
        SettingsManager().readString("exclude", context) { excluded ->
            onResponse(excluded.split(";"))
        }
    }

    fun addExcludedApp(app: String, context: Context) {
        SettingsManager().readString("exclude", context) { oldExclude ->
            SettingsManager().saveString("exclude", "$oldExclude;$app", context)
        }
    }
}