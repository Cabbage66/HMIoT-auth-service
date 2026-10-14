package com.hmiot.app.feature.terminal.domain

import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import java.io.File
import java.net.NetworkInterface
import java.time.LocalDateTime
import java.util.Locale

class TerminalStatusCollector(private val context: Context) {
    fun collect(triggerScene: String = "PERIODIC", appCode: String? = null): TerminalEventLocal {
        val snapshot = collectSecuritySnapshot()
        return TerminalEventLocal(
            eventType = "TERMINAL_STATUS_SNAPSHOT",
            eventLevel = snapshot.overallLevel,
            eventTime = LocalDateTime.now().toString(),
            eventPayload = snapshot.toPayload() + mapOf(
                "triggerScene" to triggerScene,
                "appCode" to appCode
            )
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
        val isWifiSecured = if (connectionType == "WIFI") currentWifiSecured() else null

        val rooted = isRooted()
        val bootloaderUnlocked = isBootloaderUnlocked()
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
            "isBootloaderUnlocked" to bootloaderUnlocked,
            "isOsSignatureTrusted" to (!rooted && bootloaderUnlocked != true),
            "isEmulator" to emulator,
            "isAdbEnabled" to adbEnabled,
            "hasHookRisk" to hookRisk,
            "apiLevel" to Build.VERSION.SDK_INT,
            "securityPatch" to securityPatch,
            "connectionType" to connectionType,
            "isVpnActive" to isVpnActive,
            "isWifiSecured" to isWifiSecured,
            "hasHighRiskApps" to hasHighRiskApps(),
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
            rooted || bootloaderUnlocked == true || emulator || hookRisk -> "CRITICAL"
            adbEnabled || isVpnActive || memInfo.lowMemory || battery in 0..10 -> "WARN"
            else -> "INFO"
        }
        return TerminalSecuritySnapshot(software, hardware, level)
    }

    private fun isRooted(): Boolean {
        val paths = listOf("/system/xbin/su", "/system/bin/su", "/sbin/su", "/system/app/Superuser.apk")
        return paths.any { File(it).exists() } || Build.TAGS?.contains("test-keys") == true
    }

    private fun isBootloaderUnlocked(): Boolean? {
        val verifiedBoot = systemProperty("ro.boot.verifiedbootstate")
        val flashLocked = systemProperty("ro.boot.flash.locked")
        val vbmetaState = systemProperty("ro.boot.vbmeta.device_state")
        return when {
            vbmetaState.equals("unlocked", ignoreCase = true) -> true
            flashLocked == "0" -> true
            verifiedBoot.equals("orange", ignoreCase = true) || verifiedBoot.equals("red", ignoreCase = true) -> true
            verifiedBoot.equals("green", ignoreCase = true) || flashLocked == "1" || vbmetaState.equals("locked", ignoreCase = true) -> false
            Build.TAGS?.contains("test-keys") == true -> true
            else -> null
        }
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

    private fun hasHighRiskApps(): Boolean = runCatching {
        val markers = setOf(
            "de.robv.android.xposed.installer",
            "org.lsposed.manager",
            "com.topjohnwu.magisk",
            "eu.chainfire.supersu",
            "com.saurik.substrate",
            "re.frida.server"
        )
        context.packageManager.getInstalledPackages(0).any { it.packageName in markers }
    }.getOrDefault(false)

    private fun isAppSignatureTrusted(): Boolean = runCatching {
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        val info = context.packageManager.getPackageInfo(context.packageName, flags)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo != null else info.signatures?.isNotEmpty() == true
    }.getOrDefault(false)

    private fun allowUnknownSources(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)
        } else {
            runCatching { Settings.Secure.getInt(context.contentResolver, Settings.Secure.INSTALL_NON_MARKET_APPS, 0) == 1 }.getOrDefault(false)
        }
    }

    private fun currentWifiSecured(): Boolean? = runCatching {
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val info = wifiManager.connectionInfo ?: return@runCatching null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            info.currentSecurityType != WifiInfo.SECURITY_TYPE_OPEN &&
                info.currentSecurityType != WifiInfo.SECURITY_TYPE_OWE
        } else {
            null
        }
    }.getOrNull()

    private fun systemProperty(name: String): String? = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        clazz.getMethod("get", String::class.java).invoke(null, name)?.toString()?.takeIf { it.isNotBlank() }
    }.getOrNull()

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
