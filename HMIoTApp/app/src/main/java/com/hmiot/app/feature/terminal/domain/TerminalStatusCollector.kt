package com.hmiot.app.feature.terminal.domain

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import java.net.NetworkInterface
import java.time.LocalDateTime
import java.util.Locale

class TerminalStatusCollector(private val context: Context) {
    fun collect(): TerminalEventLocal {
        val snapshot = collectSecuritySnapshot()
        return TerminalEventLocal(
            eventType = "TERMINAL_STATUS_SNAPSHOT",
            eventLevel = snapshot.overallLevel,
            eventTime = LocalDateTime.now().toString(),
            eventPayload = snapshot.toPayload()
        )
    }

    fun collectSecuritySnapshot(): TerminalSecuritySnapshot {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val memInfo = android.app.ActivityManager.MemoryInfo().also { activityManager.getMemoryInfo(it) }
        val stat = StatFs(Environment.getDataDirectory().absolutePath)
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        val battery = batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivity.activeNetwork
        val caps = connectivity.getNetworkCapabilities(activeNetwork)
        val connectionType = when {
            caps == null -> "NONE"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "WIFI"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETHERNET"
            else -> "OTHER"
        }
        val isVpnActive = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
        val isWifiSecured = if (connectionType == "WIFI") true else null

        val rooted = isRooted()
        val emulator = isEmulator()
        val adbEnabled = isAdbEnabled()
        val hookRisk = hasHookRisk()
        val securityPatch = Build.VERSION.SECURITY_PATCH ?: "UNKNOWN"
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        val appVersion = packageInfo.versionName ?: "unknown"
        val appSignatureTrusted = isAppSignatureTrusted()
        val unknownSources = allowUnknownSources()

        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
        val biometricManager = BiometricManager.from(context)
        val fingerprintSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
        val faceSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
        val voiceprintSupported = context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)
        val secureHardware = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

        val software = linkedMapOf<String, Any?>(
            "isRooted" to rooted,
            "isBootloaderUnlocked" to "UNKNOWN",
            "isOsSignatureTrusted" to !rooted,
            "isEmulator" to emulator,
            "isAdbEnabled" to adbEnabled,
            "hasHookRisk" to hookRisk,
            "apiLevel" to Build.VERSION.SDK_INT,
            "securityPatch" to securityPatch,
            "connectionType" to connectionType,
            "isVpnActive" to isVpnActive,
            "isWifiSecured" to isWifiSecured,
            "hasHighRiskApps" to false,
            "isAppSignatureTrusted" to appSignatureTrusted,
            "allowUnknownSources" to unknownSources,
            "osRelease" to Build.VERSION.RELEASE,
            "lowMemory" to memInfo.lowMemory
        )

        val hardware = linkedMapOf<String, Any?>(
            "deviceIdentifier" to "${Build.BRAND}-${Build.MODEL}-$androidId",
            "deviceId" to androidId,
            "androidId" to androidId,
            "appVersion" to appVersion,
            "deviceFingerprint" to Build.FINGERPRINT,
            "clientIp" to localIp(),
            "location" to currentLocationText(),
            "fingerprintSupported" to fingerprintSupported,
            "faceSupported" to faceSupported,
            "voiceprintSupported" to voiceprintSupported,
            "secureHardware" to secureHardware,
            "localFaceCompare" to faceSupported,
            "deviceBrand" to Build.BRAND,
            "deviceModel" to Build.MODEL,
            "batteryLevel" to battery,
            "storageFreeBytes" to stat.availableBytes,
            "storageTotalBytes" to stat.totalBytes,
            "availableMem" to memInfo.availMem
        )

        val level = when {
            rooted || emulator || hookRisk -> "CRITICAL"
            adbEnabled || isVpnActive || memInfo.lowMemory || battery in 0..10 -> "WARN"
            else -> "INFO"
        }
        return TerminalSecuritySnapshot(software, hardware, level)
    }

    private fun isRooted(): Boolean {
        val paths = listOf("/system/xbin/su", "/system/bin/su", "/sbin/su")
        return paths.any { java.io.File(it).exists() }
    }

    private fun isEmulator(): Boolean {
        val f = Build.FINGERPRINT.lowercase(Locale.ROOT)
        val m = Build.MODEL.lowercase(Locale.ROOT)
        val p = Build.PRODUCT.lowercase(Locale.ROOT)
        return f.contains("generic") || f.contains("emulator") || m.contains("emulator") || p.contains("sdk")
    }

    private fun isAdbEnabled(): Boolean = runCatching {
        Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
    }.getOrDefault(false)

    private fun hasHookRisk(): Boolean = runCatching {
        Class.forName("de.robv.android.xposed.XposedBridge")
        true
    }.getOrDefault(false)

    private fun isAppSignatureTrusted(): Boolean = runCatching {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo != null else info.signatures?.isNotEmpty() == true
    }.getOrDefault(false)

    private fun allowUnknownSources(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            runCatching { Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1 }.getOrDefault(false)
        }
    }

    private fun localIp(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it.hostAddress?.contains(":") == false }
            ?.hostAddress ?: ""
    }.getOrDefault("")

    private fun currentLocationText(): String? {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!fine && !coarse) return null
        return null
    }
}

data class TerminalSecuritySnapshot(
    val software: Map<String, Any?>,
    val hardware: Map<String, Any?>,
    val overallLevel: String
) {
    fun toPayload(): Map<String, Any?> = linkedMapOf(
        "software" to software,
        "hardware" to hardware,
        "collectedAt" to LocalDateTime.now().toString()
    )
}
