package com.sajeg.questrpc.composables

import android.util.JsonReader
import com.sajeg.questrpc.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStreamReader
import java.net.URL

fun checkForUpdates(onUpdate: (changes: String) -> Unit) {
    val changelog =
        URL("https://raw.githubusercontent.com/Sajeg/quest-rpc/refs/heads/master/changelog.json")
    val versionCode = BuildConfig.VERSION_CODE
    CoroutineScope(Dispatchers.IO).launch {
        val inputStream = withContext(Dispatchers.IO) {
            changelog.openStream()
        }
        JsonReader(InputStreamReader(inputStream)).use { reader ->
            reader.beginArray()
            reader.beginObject()
            if (reader.nextName() != "versionCode") {
                return@launch
            }
            if (reader.nextInt() > versionCode) {
                reader.nextName()
                reader.nextString()
                if (reader.nextName() == "changes") {
                    onUpdate(reader.nextString())
                }
            }
        }
    }
}