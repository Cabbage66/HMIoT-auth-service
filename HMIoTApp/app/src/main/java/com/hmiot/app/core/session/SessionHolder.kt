package com.hmiot.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SessionHolder {
    private val tokenState = MutableStateFlow<String?>(null)

    val token: StateFlow<String?> = tokenState.asStateFlow()

    fun updateToken(rawToken: String?) {
        tokenState.value = rawToken
    }
}
