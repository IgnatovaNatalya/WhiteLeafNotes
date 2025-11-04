package ru.whiteleaf.notes.domain.repository

interface EncryptionRepository {
    suspend fun encryptNotebook(notebookPath: String): Result<Unit>
    suspend fun decryptNotebook(notebookPath: String): Result<Unit>
    suspend fun encryptNote(noteId: String, notebookPath: String?): Result<Unit>
    suspend fun decryptNote(noteId: String, notebookPath: String?): Result<Unit>
    fun isNotebookUnlocked(notebookPath: String): Boolean
    fun lockNotebook(notebookPath: String)
    fun clearAllKeys()
}