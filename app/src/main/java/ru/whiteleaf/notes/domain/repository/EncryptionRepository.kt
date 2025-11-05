package ru.whiteleaf.notes.domain.repository

interface EncryptionRepository {
    suspend fun encryptNotebook(notebookPath: String): Result<Unit>
    suspend fun decryptNotebook(notebookPath: String): Result<Unit>
    suspend fun encryptNote(noteId: String, notebookPath: String?): Result<Unit>
    suspend fun decryptNote(noteId: String, notebookPath: String?): Result<Unit>
    fun isNotebookUnlocked(notebookPath: String): Boolean
    fun lockNotebook(notebookPath: String)
    fun clearAllKeys()

    fun getDecryptedContent(noteId: String): String?
    fun getDecryptedTitle(noteId: String): String?
    fun cacheDecryptedContent(noteId: String, content: String, title: String)
    fun removeFromCache(noteId: String)
}