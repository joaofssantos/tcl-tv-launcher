package com.joaofsantos.tcllauncher

import android.content.Context
import android.content.Intent
import android.net.Uri

class SeriesLauncher(private val context: Context) {
    fun open(deepLink: String) {
        val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse(deepLink)).apply {
            setPackage(PRIME_PACKAGE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(webIntent)
        } catch (_: Exception) {
            context.packageManager.getLaunchIntentForPackage(PRIME_PACKAGE)?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(it)
            }
        }
    }
    companion object { const val PRIME_PACKAGE = "com.amazon.amazonvideo.livingroom" }
}
