package ru.whiteleaf.notes.domain.repository

import android.content.Context

interface EncryptionRepository {
    // Управление ключами
    fun hasKey(notebookPath: String): Boolean

    @Throws(Exception::class)
    fun createKeyForNotebook(notebookPath: String)

    fun deleteKeyForNotebook(notebookId: String)

    // Управление состоянием разблокировки
    fun isUnlocked(notebookPath: String): Boolean
    suspend fun unlockNotebook(notebookPath: String, context: Context): Boolean
    fun lockNotebook(notebookPath: String)
    fun lockAllNotebooks()

    // Криптографические операции над содержимым заметок
    suspend fun encryptNote(notebookPath: String, plaintext: String): String
    suspend fun decryptNote(notebookPath: String, ciphertext: String): String
}

class KeyNotUnlockedException(message: String) : Exception(message)