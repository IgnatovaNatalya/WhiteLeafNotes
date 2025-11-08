package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository

class ClearNotebookKeysUseCase(private val repository: EncryptionRepository) {
    suspend operator fun invoke(notebookPath: String) {
        repository.clearNotebookKeys(notebookPath)
    }
}