package com.sajeg.questrpc.classes

import android.util.JsonReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL

class MetaDataDownloader {
    val listURL = URL("https://files.cocaine.trade/LauncherIcons/oculus_apps.json")
    val iconBaseUrl = "https://files.cocaine.trade/LauncherIcons/oculus_icon/"

    fun getAppsName(packageNames: List<String>, onFinished: (List<AppName>) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            val customNames = mutableListOf<AppName>()
            val inputStream = withContext(Dispatchers.IO) {
                listURL.openStream()
            }
            JsonReader(InputStreamReader(inputStream)).use { reader ->
                reader.beginArray()
                while (reader.hasNext()) {
                    reader.beginObject()
                    var name: String? = null
                    var packageName: String? = null
                    if (reader.nextName() == "appName") {
                        name = reader.nextString()
                    }
                    if (reader.nextName() == "packageName") {
                        packageName = reader.nextString()
                        if (packageNames.contains(packageName) && name != null) {
                            customNames.add(
                                AppName(packageName, name)
                            )
                        }
                    }
                    reader.nextName()
                    reader.nextString()
                    reader.endObject()
                }
                reader.endArray()
            }
            onFinished(customNames)
        }
    }
}