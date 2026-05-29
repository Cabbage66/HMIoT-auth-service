package com.hmiot.app.feature.terminal.domain

import java.util.Locale

class TerminalPayloadCleaner {
    private val sensitiveMarkers = listOf("password", "token", "secret", "biometric", "fingerprint", "sample", "audio", "image")

    fun clean(event: TerminalEventLocal): TerminalEventLocal {
        return event.copy(
            eventType = normalizeType(event.eventType),
            eventLevel = normalizeLevel(event.eventLevel, event.eventPayload),
            eventPayload = sanitizeMap(event.eventPayload, 40)
        )
    }

    private fun normalizeType(type: String): String {
        val t = type.trim().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_:-]"), "_")
        return if (t.length > 64) t.substring(0, 64) else t
    }

    private fun normalizeLevel(level: String, payload: Map<String, Any?>): String {
        val l = level.trim().uppercase(Locale.ROOT)
        if (l in setOf("INFO", "WARN", "ERROR", "CRITICAL")) return l
        val marker = payload.toString().lowercase(Locale.ROOT)
        return when {
            "critical" in marker || "tamper" in marker -> "CRITICAL"
            "error" in marker || "deny" in marker -> "ERROR"
            "warn" in marker || "risk" in marker -> "WARN"
            else -> "INFO"
        }
    }

    private fun sanitizeMap(source: Map<String, Any?>, maxKeys: Int): Map<String, Any?> {
        val out = linkedMapOf<String, Any?>()
        var count = 0
        for ((k, v) in source) {
            if (count >= maxKeys) break
            if (isSensitive(k)) continue
            out[k] = sanitizeValue(v)
            count++
        }
        return out
    }

    private fun sanitizeValue(value: Any?): Any? = when (value) {
        null, is Number, is Boolean -> value
        is Map<*, *> -> sanitizeMap(value.entries.associate { it.key.toString() to it.value }, 20)
        is Iterable<*> -> value.take(20).map { sanitizeValue(it) }
        else -> value.toString().take(512)
    }

    private fun isSensitive(key: String): Boolean {
        val lower = key.lowercase(Locale.ROOT)
        return sensitiveMarkers.any { it in lower }
    }
}
