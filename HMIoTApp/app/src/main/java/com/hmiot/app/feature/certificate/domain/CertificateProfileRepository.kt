package com.hmiot.app.feature.certificate.domain

import com.hmiot.app.core.storage.datastore.AppPreferenceStore
import com.hmiot.app.core.storage.datastore.LocalCertificateProfile
import kotlinx.coroutines.flow.Flow

class CertificateProfileRepository(
    private val store: AppPreferenceStore
) {
    val profile: Flow<LocalCertificateProfile?> = store.certificateProfile

    suspend fun save(profile: LocalCertificateProfile) {
        store.updateCertificateProfile(profile)
    }

    suspend fun clear() {
        store.updateCertificateProfile(null)
    }
}
