package ru.whiteleaf.notes.domain.use_case.encryption

import ru.whiteleaf.notes.domain.repository.EncryptionRepository

class IsNotebookProtectedUseCase(private val encryptionRepository: EncryptionRepository) {
    operator fun invoke(notebookPath: String): Boolean {
        return encryptionRepository.hasKey(notebookPath)
    }
}