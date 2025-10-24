package ru.whiteleaf.notes.data.repository

import javax.crypto.Cipher

// Data class для запроса биометрии
data class BiometricRequest(
    val notebookPath: String?,
    val cipher: Cipher,
    val onSuccess: () -> Unit,
    val onError: () -> Unit
)