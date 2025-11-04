package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class CheckNotebookAccessUseCase(
    private val securityPreferences: SecurityPreferences,
    private val encryptionRepository: EncryptionRepository
) {
    suspend operator fun invoke(notebookPath: String): Boolean {
        val isEncrypted = securityPreferences.isNotebookEncrypted(notebookPath)
        if (!isEncrypted) return true

        return securityPreferences.isNotebookUnlocked(notebookPath) &&
                encryptionRepository.isNotebookUnlocked(notebookPath)
    }

    fun isNotebookEncrypted(notebookPath: String): Boolean {
        return securityPreferences.isNotebookEncrypted(notebookPath)
    }
}