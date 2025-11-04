package ru.whiteleaf.notes.domain.repository

interface SecurityPreferences {
    fun setNotebookEncrypted(notebookPath: String, encrypted: Boolean)
    fun isNotebookEncrypted(notebookPath: String): Boolean
    fun setNotebookUnlocked(notebookPath: String, unlocked: Boolean)
    fun isNotebookUnlocked(notebookPath: String): Boolean
    fun clearUnlockedState()
}