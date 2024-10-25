package com.sajeg.questrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
import com.sajeg.questrpc.composables.SignInDiscord
import com.sajeg.questrpc.composables.getInstalledNonSystemApps
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
    Row (
        modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ){
        LeftScreen(Modifier)
        RightScreen(Modifier)
    }
}

@Composable
fun LeftScreen(modifier: Modifier) {
    var signIn by remember { mutableStateOf(false) }
    var tokenPresent by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        SettingsManager().readString("token", context) { token ->
            tokenPresent = token.length > 5
        }
    }


    if (signIn) {
        SignInDiscord { token ->
            signIn = false
            tokenPresent = true
            SettingsManager().saveString("token", token.toString(), context)
        }
        return
    }

    Column(
        modifier = modifier
    ) {
        Button({ signIn = true }) { Text("Sign in to Discord") }
        if (tokenPresent) {
            Text("You are signed in", color = Color(0xFF4C9306))
        }
        Button({
            val intent = Intent("android.settings.ACCESSIBILITY_SETTINGS");
            intent.setPackage("com.android.settings");
            startActivity(context, intent, null)
        }) { Text("Give accessibility service permission") }
    }
}

@Composable
fun RightScreen(modifier: Modifier) {
    val context = LocalContext.current
    var excludedApps = listOf<String>()
    var customNames = listOf<AppName>()

    LaunchedEffect(Unit) {
        AppManager().getExcludedApps(context) {
            excludedApps = it
        }
        AppManager().getCustomAppNames(context) {
            customNames = it
        }
    }

    val apps = getInstalledNonSystemApps(context).toMutableList()
    LazyColumn {
        items(apps) { app ->
            if (excludedApps.contains(app.packageName)) {
                return@items
            }
            Row {
                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                Column {
                    Text("packages: ${app.packageName}")
                    Text("name: $name")
                    customNames.forEach { appName ->
                        if (appName.packageName == app.packageName) {
                            Text("custom: ${appName.name}")
                        }
                    }
                }
            }
            Row {
                var newName by remember { mutableStateOf("") }
                TextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Enter new Name") }
                )
                Button({
                    AppManager().addCustomAppName(
                        AppName(
                            app.packageName,
                            newName,
                        ),
                        context
                    )
                }) { Text("Save") }
            }
            Row {
                Button({
                    AppManager().addExcludedApp(
                        app.packageName,
                        context
                    )
                }) { Text("Exclude") }
            }
        }
    }
}

