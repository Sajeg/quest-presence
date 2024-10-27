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
        SettingsManager().saveString("service", "closed", context)
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
                                createActivity(name.name, context)
                                return@getCustomAppNames
                            }
                        }
                        AppManager().getStoreNames(context) { savedStoreNames ->
                            savedStoreNames.forEach { name ->
                                if (name.packageName == packageName) {
                                    Log.d("NowPlayingS", name.name)
                                    createActivity(name.name, context)
                                    return@getStoreNames
                                }
                            }
                            val appName = getAppNameFromPackageName(packageName.toString(), context)
                            Log.d("NowPlayingA", appName)
                            createActivity(appName, context)
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

    fun createActivity(name: String, context: Context) {
        if (rpc == null) {
            return
        }
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
                    largeImage = "mp:attachments/1196570473601962112/1299070035083399239/Meta.png?ex=671bdcbf&is=671a8b3f&hm=55b5ec49fe77741edb34d2f65aa645f65f7c6e13be6c549c45569a3ba405b7f1&",
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