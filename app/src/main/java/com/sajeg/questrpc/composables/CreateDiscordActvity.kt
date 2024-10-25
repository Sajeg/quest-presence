package com.sajeg.questrpc.composables

import android.content.Context
import android.provider.Settings
import com.my.kizzyrpc.KizzyRPC
import com.my.kizzyrpc.model.Activity
import com.my.kizzyrpc.model.Assets

fun createActivity(rpc: KizzyRPC, name: String, context: Context) {
    rpc.setActivity(
        activity = Activity(
            applicationId = "1299052584761561161",
            name = name,
            details = "on a Meta ${
                Settings.Global.getString(
                    context.contentResolver,
                    "device_name"
                ) ?: "Unknown Device"
            }",
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