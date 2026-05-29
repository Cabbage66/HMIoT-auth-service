package com.hmiot.app.feature.certificate.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.hmiot.app.core.storage.datastore.LocalCertificateProfile
import com.hmiot.app.feature.certificate.domain.CertificateProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CertificateViewModel(
    private val repository: CertificateProfileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(CertificateUiState())
    val state: StateFlow<CertificateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.profile.collect { profile ->
                if (profile != null) {
                    _state.update {
                        it.copy(
                            serialNumber = profile.serialNumber,
                            fingerprint = profile.fingerprintSha256,
                            pem = profile.certificatePem
                        )
                    }
                }
            }
        }
    }

    fun updateSerial(value: String) = _state.update { it.copy(serialNumber = value) }
    fun updateFingerprint(value: String) = _state.update { it.copy(fingerprint = value) }
    fun updatePem(value: String) = _state.update { it.copy(pem = value) }

    fun save() {
        val s = _state.value
        if (s.serialNumber.isBlank()) {
            _state.update { it.copy(error = "证书序列号不能为空") }
            return
        }
        viewModelScope.launch {
            repository.save(
                LocalCertificateProfile(
                    serialNumber = s.serialNumber.trim(),
                    fingerprintSha256 = s.fingerprint.trim(),
                    certificatePem = s.pem.trim()
                )
            )
            _state.update { it.copy(info = "证书资料已保存到本地安全存储", error = null) }
        }
    }

    fun clear() {
        viewModelScope.launch {
            repository.clear()
            _state.update { CertificateUiState(info = "证书资料已清除") }
        }
    }
}

data class CertificateUiState(
    val serialNumber: String = "",
    val fingerprint: String = "",
    val pem: String = "",
    val info: String? = null,
    val error: String? = null
)

class CertificateViewModelFactory(
    private val repository: CertificateProfileRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CertificateViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CertificateViewModel(repository) as T
        }
        throw IllegalArgumentException("Unsupported ViewModel: $modelClass")
    }
}
