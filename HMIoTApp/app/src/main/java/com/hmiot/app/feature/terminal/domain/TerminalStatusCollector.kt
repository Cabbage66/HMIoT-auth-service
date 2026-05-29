package com.hmiot.app.feature.terminal.domain

import android.content.Context
import android.net.ConnectivityManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import java.time.LocalDateTime

class TerminalStatusCollector(private val context: Context) {
    fun collect(): TerminalEventLocal {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val active = connectivity.activeNetworkInfo
        val payload = mapOf(
            "deviceBrand" to Build.BRAND,
            "deviceModel" to Build.MODEL,
            "osRelease" to Build.VERSION.RELEASE,
            "sdkInt" to Build.VERSION.SDK_INT,
            "availableMem" to memInfo.availMem,
            "lowMemory" to memInfo.lowMemory,
            "storageFreeBytes" to stat.availableBytes,
            "storageTotalBytes" to stat.totalBytes,
            "batteryLevel" to battery,
            "networkType" to (active?.typeName ?: "UNKNOWN"),
            "networkConnected" to (active?.isConnected ?: false),
            "collectedAt" to LocalDateTime.now().toString()
        )
        val level = when {
            battery in 0..10 -> "WARN"
            memInfo.lowMemory -> "WARN"
            else -> "INFO"
        }
        return TerminalEventLocal(
            eventType = "TERMINAL_STATUS_SNAPSHOT",
            eventLevel = level,
            eventTime = LocalDateTime.now().toString(),
            eventPayload = payload
        )
    }
}
