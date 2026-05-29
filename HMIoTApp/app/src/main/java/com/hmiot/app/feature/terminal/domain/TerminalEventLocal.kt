package com.hmiot.app.feature.terminal.domain

data class TerminalEventLocal(
    val eventType: String,
    val eventLevel: String,
    val eventTime: String,
    val eventPayload: Map<String, Any?>
)
