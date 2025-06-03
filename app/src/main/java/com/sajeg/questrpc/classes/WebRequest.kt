package com.sajeg.questrpc.classes

import android.util.Log
import com.sajeg.questrpc.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject

class WebRequest() {
    var lastPackage = ""
    var cachedURL = ""

    fun getImageUrl(packageName: String, onResponse: (url: String?) -> Unit) {
        if (packageName == lastPackage) {
            onResponse(cachedURL)
            return
        }
        val client = OkHttpClient()
        val imageRequest = Request.Builder()
            .url("https://files.cocaine.trade/LauncherIcons/oculus_icon/$packageName.jpg")
            .build()
        var imageResponse: Response? = null
        try {
            imageResponse = client.newCall(imageRequest).execute()
        } catch (e: Exception) {
            onResponse(null)
            return
        }
        if (imageResponse.code == 404) {
            onResponse(null)
            return
        }
        val imageType = imageResponse.body?.contentType()
        val imageData = imageResponse.body?.bytes()

        val formBody = MultipartBody.Builder().apply {
            setType(MultipartBody.FORM)
            addFormDataPart("files[0]", "icon.jpg", imageData!!.toRequestBody(imageType))
        }.build()
        val request = Request.Builder().apply {
            url("https://canary.discord.com/api/webhooks/1379442109240574063/${BuildConfig.webhook}")
            post(formBody)
        }.build()
        val response = client.newCall(request).execute()
        val body = response.body!!.string()
        Log.d(
            "HttpResponse",
            body
        )
        if (response.code != 200) {
            onResponse(null)
            return
        }

        val json = JSONObject(body)
        val attachmentsArray = json.getJSONArray("attachments")
        val firstAttachment = attachmentsArray.getJSONObject(0)
        val url = firstAttachment.getString("url").toString()
            .replace("https://cdn.discordapp.com/", "mp:")
        lastPackage = packageName
        cachedURL = url
        onResponse(url)
    }
}