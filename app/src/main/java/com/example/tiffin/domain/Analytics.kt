package com.example.tiffin.domain

import android.util.Log

interface Analytics {
    fun track(event: String, properties: Map<String, String> = emptyMap())
}

class LogcatAnalytics : Analytics {
    override fun track(event: String, properties: Map<String, String>) {
        Log.d("TiffinAnalytics", "event=$event props=$properties")
    }
}