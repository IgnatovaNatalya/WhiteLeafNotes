package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository

class IsNotebookUnlockedUseCase(private val encryptionRepository: EncryptionRepository) {
    operator fun invoke(notebookPath: String): Boolean {
        return encryptionRepository.isUnlocked(notebookPath)
    }
}