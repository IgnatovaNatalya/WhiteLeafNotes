package ru.whiteleaf.notes.domain.use_case.encryption

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class CountEncryptedNotebooksUseCase(
    private val notebookRepository: NotebookRepository,
    private val encryptionRepository: EncryptionRepository
) {
    suspend operator fun invoke(): Int =
        notebookRepository.getNotebooks()
            .count { encryptionRepository.hasKey(it.path) }
}
