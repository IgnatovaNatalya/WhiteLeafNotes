package ru.whiteleaf.notes.domain.use_case

import androidx.fragment.app.FragmentActivity
import ru.whiteleaf.notes.domain.repository.BiometricRepository
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class UnlockNotebookUseCase(
    private val biometricRepository: BiometricRepository,
    private val encryptionRepository: EncryptionRepository,
    private val securityPreferences: SecurityPreferences
) {
    suspend operator fun invoke(notebookPath: String, activity: FragmentActivity): Result<Unit> {
        return try {
            biometricRepository.authenticate(activity).map {
                encryptionRepository.decryptNotebook(notebookPath)
                securityPreferences.setNotebookUnlocked(notebookPath, true)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка разблокировки: ${e.message}"))
        }
    }
}