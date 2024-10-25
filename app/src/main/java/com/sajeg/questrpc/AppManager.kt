package com.sajeg.questrpc

import android.content.Context

class AppManager {
    fun getExcludedApps(context: Context, onResponse: (apps: List<String>) -> Unit) {
        SettingsManager().readString("exclude", context) { excluded ->
            onResponse(excluded.split(";"))
        }
    }

    fun addExcludedApp(app: String, context: Context, finished: () -> Unit = {}) {
        SettingsManager().readString("exclude", context) { oldExclude ->
            SettingsManager().saveString("exclude", "$oldExclude;$app", context)
        }
        finished()
    }

    fun removeExcludedApp(includedApp: String, context: Context, finished: () -> Unit = {}) {
        SettingsManager().readString("exclude", context) { oldExclude ->
            val oldAppList = oldExclude.split(";")
            val newAppList = mutableListOf<String>()
            oldAppList.forEach { app ->
                if (app != includedApp) {
                    newAppList.add(app)
                }
            }
            SettingsManager().saveString("exclude", newAppList.joinToString(";"), context)
        }
        finished()
    }

    fun getCustomAppNames(context: Context, onResponse: (names: List<AppName>) -> Unit) {
        SettingsManager().readString("name", context) { names ->
            val nameList = names.split(";")
            var appNameList = mutableListOf<AppName>()
            nameList.forEach { name ->
                try {
                    appNameList.add(AppName(name.split(",")[0], name.split(",")[1]))
                } catch (e: Exception) {

                }
            }

            onResponse(appNameList.toList())
        }
    }

    fun addCustomAppName(appName: AppName, context: Context, finished: () -> Unit = {}) {
        SettingsManager().readString("name", context) { oldNames ->
            SettingsManager().saveString("name", "$oldNames;${appName.toString()}", context)
        }
        finished()
    }
}