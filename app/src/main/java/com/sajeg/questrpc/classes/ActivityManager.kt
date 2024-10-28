package com.sajeg.questrpc.classes

import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.util.Log
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.sajeg.questrpc.composables.getInstalledVrGames

object ActivityManager {
    var rpc: KizzyRPC? = null

    fun start(context: Context) {
        SettingsManager().readString("token", context) { token ->
            rpc = KizzyRPC(token)
        }
    }

    fun stop(context: Context) {
        SettingsManager().saveString("game", "null", context)
        if (rpc != null) {
            rpc!!.closeRPC()
            rpc == null
        }
    }

    fun appChanged(packageName: String, context: Context) {
        if (rpc == null) {
            return
        }
        val apps = getInstalledVrGames(context)
        apps.forEach { vrGame ->
            if (vrGame.packageName == packageName) {
                AppManager().getExcludedApps(context) { apps ->
                    if (apps.contains(packageName)) {
                        return@getExcludedApps
                    }
                    AppManager().getCustomAppNames(context) { names ->
                        names.forEach { name ->
                            if (name.packageName == packageName) {
                                Log.d("NowPlayingC", name.name)
                                createActivity(name.name, packageName, context)
                                return@getCustomAppNames
                            }
                        }
                        AppManager().getStoreNames(context) { savedStoreNames ->
                            savedStoreNames.forEach { name ->
                                if (name.packageName == packageName) {
                                    Log.d("NowPlayingS", name.name)
                                    createActivity(name.name, packageName, context)
                                    return@getStoreNames
                                }
                            }
                            val appName = getAppNameFromPackageName(packageName.toString(), context)
                            Log.d("NowPlayingA", appName)
                            createActivity(appName, packageName, context)
                        }
                    }
                }
            }
        }
    }

    private fun getAppNameFromPackageName(packageName: String, context: Context): String {
        return try {
            val packageManager = context.packageManager
            val appInfo = packageManager.getApplicationInfo(packageName, 0)

            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            packageName
        }
    }

    fun createActivity(name: String, packageName: String, context: Context) {
        SettingsManager().saveString("game", name, context)
        if (rpc == null) {
            start(context)
        }
        WebRequest().getImageUrl(packageName) { image ->
            rpc!!.setActivity(
                activity = Activity(
                    applicationId = "1299052584761561161",
                    name = name,
                    details = "on a Meta ${
                        Settings.Global.getString(
                            context.contentResolver,
                            "device_name"
                        ) ?: "Unknown Device"
                    }",
                    type = 0,
                    assets = Assets(
                        largeImage = image,
                        smallImage = null,
                        largeText = "Meta Quest",
                        smallText = null
                    )
                ),
                status = "online",
                since = System.currentTimeMillis()
            )
        }
    }
}