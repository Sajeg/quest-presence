package com.sajeg.questrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
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
                state = "Playing $game"
            }
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
        Text("Information", style = MaterialTheme.typography.headlineLarge)
        if (tokenPresent) {
            val text = buildAnnotatedString {
                append("Signed in? ")
                withStyle(style = SpanStyle(color = Color.Green)) {
                    append("Check")
                }
                append("\nHas accessibility permission? ")
                if (serviceEnabled) {
                    withStyle(style = SpanStyle(color = Color.Green)) {
                        append("Check")
                    }
                } else {
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append("Negative")
                    }
                }
                append("\nState? ")
                if (state == "Nothing") {
                    withStyle(style = SpanStyle(color = Color.Red)) {
                        append("Playing nothing")
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
                    text = "Thank you for using Quest RPC. To use the app you need to sign in to Discord and then give the app accessibility permission. This required for the App to detect what game you are playing. I don't collect any data except because of limitations though discord I can see in a the Discord channel the logo of an game when someone starts it. \n \nNote: On first start the app names can be shown wrong. \nThat'll change with the next app start."
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
                ) { Text("Stop current game") }
            } else {
                Button(
                    modifier = Modifier.width(buttonSize),
                    onClick = { signIn = true; ActivityManager.stop(context) }
                ) { Text("Sign in to Discord") }
            }
            Button(
                modifier = Modifier.width(buttonSize),
                onClick = {
                    SettingsManager().readString("token", context) { token ->
                        if (token != "null") {
                            val intent = Intent("android.settings.ACCESSIBILITY_SETTINGS");
                            intent.setPackage("com.android.settings");
                            startActivity(context, intent, null)
                        }
                    }
                }
            ) { Text("Go to accessibility settings") }
        }

        if (changes != null) {
            Text(
                modifier = Modifier.padding(horizontal = 5.dp).padding(top = 20.dp),
                text = "New Update",
                style = MaterialTheme.typography.headlineLarge
            )
            Card (
                modifier = Modifier.fillMaxWidth()
            ){
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
                ) { Text("Open SideQuest") }
                Button(
                    modifier = Modifier.width(buttonSize),
                    onClick = {
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://github.com/Sajeg/quest-rpc")
                        )
                        startActivity(context, browserIntent, null)
                    }
                ) { Text("Open GitHub") }
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
                MetaDataDownloader().getAppsName(packageNames) { newNames ->
                    storeNames = newNames.toMutableStateList()
                    storeNames.sortedBy { it.name }
                    storeNames.forEach { storePackages.add(it.packageName) }
                    AppManager().addStoreNames(newNames, context)
                }
            } else {
                storeNames = savedStoreNames.toMutableStateList()
                storeNames.sortedBy { it.name }
                storeNames.forEach { storePackages.add(it.packageName) }
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
                Text("Unknown Apps ", style = MaterialTheme.typography.headlineLarge)
                Text("You may want to exclude them", style = MaterialTheme.typography.bodyMedium)
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
            Text("Recognized Apps ", style = MaterialTheme.typography.headlineLarge)
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
            Text("Excluded Apps ", style = MaterialTheme.typography.headlineLarge)
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
                    onChangedName(app)
                    newSavedName = newName
                    newName = ""
                }
            ) { Text("Save") }
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

