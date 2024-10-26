package com.sajeg.questrpc.classes

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.sajeg.questrpc.composables.getInstalledVrGames

class BackgroundUpdater(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val apps = getInstalledVrGames(applicationContext)
        val packageNames = mutableListOf<String>()
        apps.forEach { app ->
            packageNames.add(app.packageName)
        }
        try {
            MetaDataDownloader().getAppsName(packageNames) { newNames ->
                AppManager().addStoreNames(newNames, applicationContext)
            }
        } catch (e: Exception) {
            return Result.failure()
        }

        return Result.success()
    }
}