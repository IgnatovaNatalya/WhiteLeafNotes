package ru.whiteleaf.notes.domain.use_case.encryption

import ru.whiteleaf.notes.domain.repository.EncryptionRepository

class LockNotebookUseCase(
    private val encryptionRepository: EncryptionRepository
) {
    operator fun invoke(notebookPath: String) {
        encryptionRepository.clearUnlockedFlag(notebookPath)
    }
}