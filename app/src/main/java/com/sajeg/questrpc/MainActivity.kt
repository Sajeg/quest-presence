package com.sajeg.questrpc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat.startActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets
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
    var signIn by remember { mutableStateOf(false) }
    var tokenPresent by remember { mutableStateOf(false) }
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
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(context, intent, null)
        }) { Text("Give accessibility Service permission") }

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
}

fun createActivity(rpc: KizzyRPC, name: String) {
    rpc.setActivity(
        activity = Activity(
            applicationId = "1299052584761561161",
            name = name,
            details = "on a Meta Quest 3",
            type = 0,
            assets = Assets(
                largeImage = "mp:attachments/1196570473601962112/1299070035083399239/Meta.png?ex=671bdcbf&is=671a8b3f&hm=55b5ec49fe77741edb34d2f65aa645f65f7c6e13be6c549c45569a3ba405b7f1&",
                smallImage = null,
                largeText = "Meta Quest",
                smallText = null
            )
        ),
        status = "online",
        since = System.currentTimeMillis()
    )
}

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

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun SignInDiscord(onDataRetrieved: (token: String?) -> Unit) {
    val url = "https://discord.com/login"
    val jsCode = "(webpackChunkdiscord_app.push([[''],{},e=>{m=[];for(let c in e.c)m.push(e.c[c])}]),m).find(m=>m?.exports?.default?.getToken!==void 0).exports.default.getToken()"
    AndroidView(factory = {
        WebView(it).apply {
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
            }
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            webViewClient = object : WebViewClient() {
                @Deprecated("Deprecated in Java")
                override fun shouldOverrideUrlLoading(
                    webView: WebView,
                    url: String,
                ): Boolean {
                    stopLoading()
                    if (url.endsWith("/app")) {
                        evaluateJavascript(jsCode) { value ->
                            if (value != null && !value.startsWith("Error")) {
                                val token =
                                    value.replace("\"", "")
                                onDataRetrieved(token)
                                Log.d("FetchedToken", "Success")
                            } else {
                                Log.d("FetchedToken", "Failed to fetch token: $value")
                            }
                        }
                    }
                    visibility = View.GONE
                    return false
                }
            }

            loadUrl(url)
        }
    })
}