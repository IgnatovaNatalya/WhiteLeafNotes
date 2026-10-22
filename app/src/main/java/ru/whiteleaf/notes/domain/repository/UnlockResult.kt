package ru.whiteleaf.notes.domain.repository

sealed interface UnlockResult {
    data object Success : UnlockResult
    data object UserCancelled : UnlockResult
    data object DeviceNotSecure : UnlockResult
    data class Error(val code: Int, val message: CharSequence) : UnlockResult
}