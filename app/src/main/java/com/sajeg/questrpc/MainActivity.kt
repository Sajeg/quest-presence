package com.sajeg.questrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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
import com.sajeg.questrpc.classes.ActivityManager
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

@SuppressLint("ServiceCast")
@Composable
fun LeftScreen(modifier: Modifier) {
    val context = LocalContext.current
    var signIn by remember { mutableStateOf(false) }
    var tokenPresent by remember { mutableStateOf(false) }
    var serviceEnabled by remember { mutableStateOf(false) }
    var state by remember { mutableStateOf<String>("Nothing") }
    var changes by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        SettingsManager().readString("token", context) { token ->
            tokenPresent = token.length > 5
        }
        checkForUpdates { changes = it }
        SettingsManager().readString("game", context) { game ->
            if (game != "null") {
                state = context.getString(R.string.playing, game)
            }
        }
        val prefString =
            Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        serviceEnabled = try {
            prefString.contains(
                "com.sajeg.questrpc.classes.AccessibilityService"
            )
        } catch (e: Exception) {
            false
        }

    }

    if (signIn) {
        SignInDiscord { token ->
            signIn = false
            tokenPresent = true
            SettingsManager().saveString("token", token.toString(), context)
            ActivityManager.start(context)
        }
        return
    }

    Column(
        modifier = modifier
            .padding(horizontal = 15.dp, vertical = 15.dp)
            .width(450.dp)
    ) {
        val buttonSize = 220.dp
        Text(stringResource(R.string.information), style = MaterialTheme.typography.headlineLarge)
        if (tokenPresent) {
            val text = buildAnnotatedString {
                append(stringResource(R.string.signed_in))
                withStyle(style = SpanStyle(color = Color.Green)) {
                    append(stringResource(R.string.check))
                }
                append(stringResource(R.string.has_accessibility_permission))
                if (serviceEnabled) {
                    withStyle(style = SpanStyle(color = Color.Green)) {
                        append(stringResource(R.string.check))
                    }
                } else {
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(stringResource(R.string.negative))
                    }
                }
                append(stringResource(R.string.state))
                if (state == "Nothing") {
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append(stringResource(R.string.playing_nothing))
                    }
                } else {
                    withStyle(style = SpanStyle(color = Color.Green)) {
                        append(state)
                    }
                }
                toAnnotatedString()
            }
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = text
                )
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = stringResource(R.string.info)
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 5.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (tokenPresent) {
                Button(
                    modifier = Modifier.width(buttonSize),
                    onClick = {
                        ActivityManager.stop(context)
                        state = "Nothing"
                    }
                ) { Text(stringResource(R.string.stop)) }
            } else {
                Button(
                    modifier = Modifier.width(buttonSize),
                    onClick = { signIn = true; ActivityManager.stop(context) }
                ) { Text(stringResource(R.string.sign_in_discord)) }
            }
            Button(
                modifier = Modifier.width(buttonSize),
                onClick = {
                    SettingsManager().readString("token", context) { token ->
                        val intent = Intent("android.settings.ACCESSIBILITY_SETTINGS");
                        intent.setPackage("com.android.settings");
                        startActivity(context, intent, null)
                    }
                }
            ) { Text(stringResource(R.string.go_to_accessibility_settings)) }
        }

        if (changes != null) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 5.dp)
                    .padding(top = 20.dp),
                text = stringResource(R.string.new_update),
                style = MaterialTheme.typography.headlineLarge
            )
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    modifier = Modifier.padding(10.dp),
                    text = changes!!
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Button(
                    modifier = Modifier.width(buttonSize),
                    onClick = {
                        val browserIntent =
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://sdq.st/a/38617"))
                        startActivity(context, browserIntent, null)
                    }
                ) { Text(stringResource(R.string.open_sidequest)) }
                Button(
                    modifier = Modifier.width(buttonSize),
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Sajeg/quest-presence")
                        )
                        startActivity(context, browserIntent, null)
                    }
                ) { Text(stringResource(R.string.open_github)) }
            }
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
    var storePackages = remember { mutableStateListOf<String>() }
    var refreshNames by remember { mutableStateOf(false) }
    val packageManager = context.packageManager

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
                val updateWorkRequest =
                    PeriodicWorkRequestBuilder<BackgroundUpdater>(6, TimeUnit.HOURS)
                        .setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.UNMETERED)
                                .build()
                        )
                        .build()
                WorkManager
                    .getInstance(context)
                    .enqueueUniquePeriodicWork(
                        "updateNames",
                        ExistingPeriodicWorkPolicy.KEEP,
                        updateWorkRequest
                    )
                MetaDataDownloader().getAppsName(packageNames) { newNames ->
                    AppManager().addStoreNames(newNames, context)
                    storeNames = newNames.toMutableStateList()
                    storeNames.sortedBy { it.name }
                    storeNames.forEach { storePackages.add(it.packageName) }
                    val intent = Intent(context, MainActivity::class.java)
                    startActivity(context, intent, null)
                }
            } else {
                storeNames = savedStoreNames.toMutableStateList()
                storeNames.sortBy { it.name }
                storeNames.forEach { storePackages.add(it.packageName) }
                refreshNames = true
            }
        }
    }
    LazyColumn(
        modifier = modifier
            .padding(15.dp)
            .animateContentSize()
    ) {
        item {
            Column {
                Text(
                    stringResource(R.string.unknown_apps),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    stringResource(R.string.unknown_tipp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        items(apps, { it }) { appInfo ->
            if (excludedApps.contains(appInfo.packageName) ||
                newExcludedApps.contains(appInfo.packageName) ||
                storePackages.contains(appInfo.packageName)
            ) {
                return@items
            }
            val appInfo = packageManager.getApplicationInfo(appInfo.packageName, 0)
            val name by remember {
                mutableStateOf(
                    packageManager.getApplicationLabel(appInfo).toString()
                )
            }
            val app = AppName(appInfo.packageName, name)
            customNames.forEach { appName ->
                if (appName.packageName == app.packageName) {
                    app.name = appName.name
                }
            }
            newCustomNames.forEach { appName ->
                if (appName.packageName == app.packageName) {
                    app.name = appName.name
                }
            }

            AppCard(context, app, { newCustomNames.add(it) }, { newExcludedApps.add(it) })
        }
        item {
            Text(
                stringResource(R.string.recognized_apps),
                style = MaterialTheme.typography.headlineLarge
            )
        }
        items(storeNames, { it.packageName }) { app ->
            if (excludedApps.contains(app.packageName) || newExcludedApps.contains(app.packageName)) {
                return@items
            }
            customNames.forEach { appName ->
                if (appName.packageName == app.packageName) {
                    app.name = appName.name
                }
            }
            newCustomNames.forEach { appName ->
                if (appName.packageName == app.packageName) {
                    app.name = appName.name
                }
            }

            AppCard(context, app, { newCustomNames.add(it) }, { newExcludedApps.add(it) })
        }

        item {
            Text(
                stringResource(R.string.excluded_apps),
                style = MaterialTheme.typography.headlineLarge
            )
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
private fun AppCard(
    context: Context,
    app: AppName,
    onChangedName: (newName: AppName) -> Unit,
    onExcluded: (packageName: String) -> Unit
) {
    Card(
        modifier = Modifier
            .width(600.dp)
            .padding(vertical = 10.dp)
    ) {
        var newName by remember { mutableStateOf("") }
        var newSavedName by remember { mutableStateOf<String?>(null) }

        Row(
            modifier = Modifier
                .padding(horizontal = 15.dp)
                .padding(top = 15.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.weight(0.1f)
            ) {
                if (newSavedName != null) {
                    Text(newSavedName!!, style = MaterialTheme.typography.headlineMedium)
                } else {
                    Text(app.name, style = MaterialTheme.typography.headlineMedium)
                }
                Text(app.packageName, style = MaterialTheme.typography.bodyMedium)
            }

            Button(
                modifier = Modifier.width(100.dp),
                onClick = {
                    AppManager().addExcludedApp(app.packageName, context)
                    onExcluded(app.packageName)
                }) { Text(stringResource(R.string.exclude)) }
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
                label = { Text(stringResource(R.string.override_app_name)) },
                maxLines = 1
            )

            Button(
                modifier = Modifier.width(100.dp),
                onClick = {
                    val newApp = AppName(app.packageName, newName)
                    AppManager().addCustomAppName(newApp, context)
                    onChangedName(app)
                    newSavedName = newName
                    newName = ""
                }
            ) { Text(stringResource(R.string.save)) }
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
                Text(stringResource(R.string.include))
            }
        }
    }
}

