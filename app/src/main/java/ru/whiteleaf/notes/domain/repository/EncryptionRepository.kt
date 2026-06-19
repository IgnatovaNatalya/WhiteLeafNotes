package ru.whiteleaf.notes.domain.repository

import android.content.Context

interface EncryptionRepository {
    // Управление ключами
    fun hasKey(notebookPath: String): Boolean

    @Throws(Exception::class)
    fun createKeyForNotebook(notebookPath: String)

    fun deleteKeyForNotebook(notebookPath: String)

    // Управление состоянием разблокировки
    fun isUnlocked(notebookPath: String): Boolean

    suspend fun unlockNotebook(notebookPath: String, context: Context,reason: String): Boolean

    fun clearUnlockedFlag(notebookPath: String)
    fun lockAllNotebooks()

    // Криптографические операции над содержимым заметок
    suspend fun encryptNote(notebookPath: String, plaintext: String): String
    suspend fun decryptNote(notebookPath: String, ciphertext: String): String
}

class AuthenticationRequiredException(message: String) : Exception(message)