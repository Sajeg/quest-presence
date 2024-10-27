package com.sajeg.questrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.sajeg.questrpc.classes.AppManager
import com.sajeg.questrpc.classes.AppName
import com.sajeg.questrpc.classes.BackgroundUpdater
import com.sajeg.questrpc.classes.MetaDataDownloader
import com.sajeg.questrpc.classes.SettingsManager
import com.sajeg.questrpc.composables.SignInDiscord
import com.sajeg.questrpc.composables.checkForUpdates
import com.sajeg.questrpc.composables.getInstalledVrGames
import com.sajeg.questrpc.ui.theme.QuestRPCTheme
import java.util.concurrent.TimeUnit

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            QuestRPCTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Main(Modifier.padding(innerPadding))
                    val uploadWorkRequest =
                        PeriodicWorkRequestBuilder<BackgroundUpdater>(6, TimeUnit.HOURS)
                            .setConstraints(
                                Constraints.Builder()
                                    .setRequiredNetworkType(NetworkType.UNMETERED)
                                    .build()
                            )
                            .build()
                    WorkManager
                        .getInstance(this)
                        .enqueueUniquePeriodicWork(
                            "updateNames",
                            ExistingPeriodicWorkPolicy.KEEP,
                            uploadWorkRequest
                        )
                }
            }
        }
    }
}

@SuppressLint("QueryPermissionsNeeded")
@Composable
fun Main(modifier: Modifier) {
    Row(
        modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        LeftScreen(Modifier)
        RightScreen(Modifier)
    }
}

@Composable
fun LeftScreen(modifier: Modifier) {
    val context = LocalContext.current
    var signIn by remember { mutableStateOf(false) }
    var tokenPresent by remember { mutableStateOf(false) }
    var changes by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SettingsManager().readString("token", context) { token ->
            tokenPresent = token.length > 5
        }
        checkForUpdates { changes = it }
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
        Text(
            modifier = Modifier.padding(10.dp),
            text = "Thank you for using Quest RPC. \nTo get started sign in to Discord \nand give the app accessibility permission."
        )
        Button({ signIn = true }) { Text("Sign in to Discord") }
        if (tokenPresent) {
            Text("You are signed in", color = Color(0xFF4C9306))
        }
        Button({
            val intent = Intent("android.settings.ACCESSIBILITY_SETTINGS");
            intent.setPackage("com.android.settings");
            startActivity(context, intent, null)
        }) { Text("Give accessibility service permission") }
        Spacer(Modifier.height(30.dp))
        if (changes != null) {
            Text(modifier = Modifier.width(280.dp), text = "Update available. Changes: \n\n $changes")
        }
    }
}

@Composable
fun RightScreen(modifier: Modifier) {
    val context = LocalContext.current
    var excludedApps = remember { mutableStateListOf<String>() }
    var customNames = mutableListOf<AppName>()
    var newExcludedApps = remember { mutableStateListOf<String>() }
    var newCustomNames = remember { mutableStateListOf<AppName>() }
    var apps = remember { mutableStateListOf<ApplicationInfo>() }
    var storeNames = remember { mutableStateListOf<AppName>() }

    if (excludedApps.isEmpty()) {
        AppManager().getExcludedApps(context) {
            excludedApps = it.toMutableStateList()
        }
    }
    if (customNames.isEmpty()) {
        AppManager().getCustomAppNames(context) {
            customNames = it.toMutableList()
        }
    }
    if (apps.isEmpty()) {
        apps = getInstalledVrGames(context).toMutableStateList()
        AppManager().getStoreNames(context) { savedStoreNames ->
            if (savedStoreNames.isEmpty()) {
                val packageNames = mutableListOf<String>()
                apps.forEach { app ->
                    packageNames.add(app.packageName)
                }
                MetaDataDownloader().getAppsName(packageNames) { newNames ->
                    storeNames = newNames.toMutableStateList()
                    AppManager().addStoreNames(newNames, context)
                }
            } else {
                storeNames = savedStoreNames.toMutableStateList()
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .padding(15.dp)
            .animateContentSize()
    ) {
        item {
            Text("Apps: ", style = MaterialTheme.typography.headlineLarge)
        }
        items(apps, { it }) { app ->
            if (excludedApps.contains(app.packageName) || newExcludedApps.contains(app.packageName)) {
                return@items
            }
            Card(
                modifier = Modifier
                    .width(600.dp)
                    .padding(vertical = 10.dp)
            ) {
                val packageManager = context.packageManager
                val appInfo = packageManager.getApplicationInfo(app.packageName, 0)
                val name = packageManager.getApplicationLabel(appInfo).toString()
                var newName by remember { mutableStateOf("") }
                var customAppName by remember { mutableStateOf<String?>(null) }

                storeNames.forEach { appName ->
                    if (appName.packageName == app.packageName) {
                        customAppName = appName.name
                    }
                }
                customNames.forEach { appName ->
                    if (appName.packageName == app.packageName) {
                        customAppName = appName.name
                    }
                }
                newCustomNames.forEach { appName ->
                    if (appName.packageName == app.packageName) {
                        customAppName = appName.name
                    }
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 15.dp)
                        .padding(top = 15.dp)
                        .fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(0.1f)
                    ) {
                        if (customAppName == null) {
                            Text(name, style = MaterialTheme.typography.headlineMedium)
                        } else {
                            Text(customAppName!!, style = MaterialTheme.typography.headlineMedium)
                        }
                        Text(app.packageName, style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        modifier = Modifier.width(100.dp),
                        onClick = {
                            AppManager().addExcludedApp(app.packageName, context)
                            newExcludedApps.add(app.packageName)
                        }) { Text("Exclude") }
                }
                Row(
                    modifier = Modifier
                        .padding(horizontal = 15.dp)
                        .padding(bottom = 15.dp)
                        .fillMaxWidth()
                ) {
                    TextField(
                        modifier = Modifier.weight(0.1f),
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Override app name") },
                        maxLines = 1
                    )

                    Button(
                        modifier = Modifier.width(100.dp),
                        onClick = {
                            val newApp = AppName(app.packageName, newName)
                            AppManager().addCustomAppName(newApp, context)
                            newCustomNames.add(newApp)
                            newName = ""
                        }
                    ) { Text("Save") }
                }
            }
        }

        item {
            Text("Excluded Apps: ", style = MaterialTheme.typography.headlineLarge)
        }
        items(newExcludedApps, { it }) { packageName ->
            ExcludedAppsCard(packageName, context, modifier) {
                AppManager().removeExcludedApp(packageName, context)
                newExcludedApps.remove(packageName)
            }
        }
        items(excludedApps, { it }) { packageName ->
            ExcludedAppsCard(packageName, context, modifier) {
                AppManager().removeExcludedApp(packageName, context)
                excludedApps.remove(packageName)
            }
        }
    }
}

@Composable
private fun ExcludedAppsCard(
    packageName: String,
    context: Context,
    modifier: Modifier,
    onIncludePressed: (packageName: String) -> Unit
) {
    if (packageName == "null") {
        return
    }
    val packageManager = context.packageManager
    val appInfo = packageManager.getApplicationInfo(packageName, 0)
    val name = packageManager.getApplicationLabel(appInfo).toString()
    Card(
        modifier = Modifier
            .padding(vertical = 5.dp)
            .width(600.dp)
    ) {
        Row(
            modifier = modifier.padding(15.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(0.1f)
            ) {
                Text(name, style = MaterialTheme.typography.bodyLarge)
                Text(packageName, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                modifier = Modifier.width(100.dp),
                onClick = { onIncludePressed(packageName) }
            ) {
                Text("Include")
            }
        }
    }
}

