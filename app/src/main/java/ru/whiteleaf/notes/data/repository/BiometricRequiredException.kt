package ru.whiteleaf.notes.data.repository


class BiometricRequiredException(
    val notebookPath: String?,
    val keyAlias: String,
    cause: Throwable? = null
) : Exception("Biometric authentication required for notebook: $notebookPath", cause)
