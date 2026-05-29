package com.hmiot.app.core.session

import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.core.storage.datastore.LocalSession
import kotlinx.coroutines.flow.Flow

class SessionManager(
    private val preferenceStore: AppPreferenceStore
) {
    val session: Flow<LocalSession?> = preferenceStore.session

    suspend fun saveSession(session: LocalSession) {
        preferenceStore.updateSession(session)
    }

    suspend fun clearSession() {
        preferenceStore.updateSession(null)
    }
}
