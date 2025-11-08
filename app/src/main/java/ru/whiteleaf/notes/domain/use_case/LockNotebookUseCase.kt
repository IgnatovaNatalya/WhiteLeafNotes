package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class LockNotebookUseCase(
    private val encryptionRepository: EncryptionRepository,
    private val securityPreferences: SecurityPreferences
) {
    operator fun invoke(notebookPath: String) {
        println("🔒 Выполняем блокировку блокнота: $notebookPath")

        encryptionRepository.lockNotebook(notebookPath)
        securityPreferences.setNotebookUnlocked(notebookPath, false)

        println("✅ Блокнот заблокирован")
    }
}