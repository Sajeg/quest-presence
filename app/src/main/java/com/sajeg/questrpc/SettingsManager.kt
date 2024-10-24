package com.sajeg.questrpc

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SettingsManager {
    fun saveString(id: String, value: String, context: Context) {
        val idPreferenceKey = stringPreferencesKey(id)
        CoroutineScope(Dispatchers.IO).launch {
            context.dataStore.edit { settings ->
                settings[idPreferenceKey] = value
            }
        }
    }

    fun readString(id: String, context: Context, onResponse: (value: String) -> Unit) {
        val idPreferenceKey = stringPreferencesKey(id)
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = context.dataStore.data.first()
            val data = preferences[idPreferenceKey]
            onResponse(data.toString())
        }
    }
}