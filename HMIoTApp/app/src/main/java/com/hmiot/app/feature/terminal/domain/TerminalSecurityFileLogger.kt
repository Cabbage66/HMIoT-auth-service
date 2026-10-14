package com.hmiot.app.feature.terminal.domain

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.time.LocalDateTime
import java.util.UUID

class TerminalSecurityFileLogger(context: Context) {
    private val gson = Gson()
    private val logFile = File(File(context.filesDir, "security-event-logs"), "terminal-security.jsonl")

    fun append(event: TerminalEventLocal): String = runCatching {
        logFile.parentFile?.mkdirs()
        val entry = linkedMapOf<String, Any?>(
            "localLogId" to UUID.randomUUID().toString(),
            "writtenAt" to LocalDateTime.now().toString(),
            "eventType" to event.eventType,
            "eventLevel" to event.eventLevel,
            "eventTime" to event.eventTime,
            "eventPayload" to event.eventPayload
        )
        logFile.appendText(gson.toJson(entry) + "\n")
        logFile.absolutePath
    }.getOrDefault("")

    fun fixedLogPath(): String = logFile.absolutePath
}
