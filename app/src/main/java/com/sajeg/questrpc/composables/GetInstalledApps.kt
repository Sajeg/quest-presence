package com.sajeg.questrpc.composables

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager

fun getInstalledNonSystemApps(context: Context): List<ApplicationInfo> {
    val packageManager = context.packageManager
    val apps = mutableListOf<ApplicationInfo>()

    val packages = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)

    for (packageInfo in packages) {
        if ((packageInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0 &&
            (packageInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
        ) {
            apps.add(packageInfo)
        }
    }

    return apps
}