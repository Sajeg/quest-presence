package com.sajeg.questrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.my.kizzyrpc.model.Timestamps
import com.sajeg.questrpc.ui.theme.QuestRPCTheme

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuestRPCTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Main(Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun Main(modifier: Modifier) {
    var discordToken by remember { mutableStateOf("") }
    var requiresSetup by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var excludedApps = listOf<String>()

    AppManager().getExcludedApps(context) {
        excludedApps = it
    }

    Column(
        modifier = modifier
    ) {
        SettingsManager().readString("token", context) { token ->
            if (token == "") {
                requiresSetup = true
            }
        }
        if (requiresSetup) {
            Text("First time set up:")
            TextField(
                value = discordToken,
                onValueChange = { discordToken = it },
                label = { Text("Enter Discord Token") }
            )
            Button({
                SettingsManager().saveString("token", discordToken, context)
            }) { Text("Save token") }
        }
        Button({
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(context, intent, null)
        }) { Text("Give accessibility Service permission") }

        val packageManager = context.packageManager
        val apps = packageManager.getInstalledPackages(PackageManager.GET_META_DATA).toMutableList()
        LazyColumn {
            items(apps) { app ->
                if (excludedApps.contains(app.packageName)) {
                    return@items
                }
                Row {
                    Text(app.packageName)
                    Text("")
                }
                Row {
                    var newName by remember { mutableStateOf("") }
                    TextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Enter new Name") }
                    )
                    Button({}) { Text("Save") }
                }
                Row {
                    Button({ AppManager().addExcludedApp(app.packageName, context) }) { Text("Exclude") }
                }
            }
        }
    }
}

fun createActivity(rpc: KizzyRPC, name: String) {
    rpc.setActivity(
        activity = Activity(
            applicationId = "1299052584761561161",
            name = name,
            details = "on a Meta Quest 3",
            state = "Call me there",
            type = 0,
            timestamps = Timestamps(
                start = System.currentTimeMillis(),
                end = System.currentTimeMillis() + 500000
            ),
            assets = Assets(
                largeImage = "mp:meta",
                smallImage = "mp:attachments/973256105515974676/983674644823412798/unknown.png",
                largeText = "large-image-text",
                smallText = "small-image-text",
            ),
        ),
        status = "online",
        since = System.currentTimeMillis()
    )
}