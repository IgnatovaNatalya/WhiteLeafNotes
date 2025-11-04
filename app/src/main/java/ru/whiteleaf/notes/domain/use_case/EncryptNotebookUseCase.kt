package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class EncryptNotebookUseCase(
    private val encryptionRepository: EncryptionRepository,
    private val securityPreferences: SecurityPreferences
) {
    suspend operator fun invoke(notebookPath: String): Result<Unit> {
        return encryptionRepository.encryptNotebook(notebookPath).map {
            securityPreferences.setNotebookEncrypted(notebookPath, true)
            // НЕ разблокируем автоматически - пользователь должен будет разблокировать отпечатком
            securityPreferences.setNotebookUnlocked(notebookPath, false)
        }
    }
}