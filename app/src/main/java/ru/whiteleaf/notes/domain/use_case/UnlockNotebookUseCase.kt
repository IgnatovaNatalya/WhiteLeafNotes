package ru.whiteleaf.notes.domain.use_case

import android.content.Context
import ru.whiteleaf.notes.domain.repository.EncryptionRepository

class UnlockNotebookUseCase(
    private val encryptionRepository: EncryptionRepository
) {
    suspend operator fun invoke(
        notebookPath: String,
        context: Context,
        reason: String = "Для просмотра"
    ): Boolean {
        return encryptionRepository.unlockNotebook(notebookPath, context, reason)
    }
}