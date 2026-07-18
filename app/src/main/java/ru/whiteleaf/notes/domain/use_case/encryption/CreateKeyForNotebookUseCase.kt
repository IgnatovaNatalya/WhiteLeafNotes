package ru.whiteleaf.notes.domain.use_case.encryption

import ru.whiteleaf.notes.domain.repository.EncryptionRepository

class CreateKeyForNotebookUseCase(
    private val encryptionRepository: EncryptionRepository
) {
    operator fun invoke(notebookPath: String) {
        encryptionRepository.createKeyForNotebook(notebookPath)
    }
}