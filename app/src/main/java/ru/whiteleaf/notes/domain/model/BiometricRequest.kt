package ru.whiteleaf.notes.domain.model

import javax.crypto.Cipher

data class BiometricRequest(
    val notebookPath: String?,
    val keyAlias: String?,
    val cipher: Cipher,
    val onSuccess: () -> Unit,
    val onError: () -> Unit
)