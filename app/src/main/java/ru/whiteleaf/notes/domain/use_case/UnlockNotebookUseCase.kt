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

            println("🔄 ПОВТОРНАЯ разблокировка блокнота: $notebookPath")

            // Проверим текущее состояние перед разблокировкой
            val currentlyEncrypted = securityPreferences.isNotebookEncrypted(notebookPath)
            val currentlyUnlocked = securityPreferences.isNotebookUnlocked(notebookPath)
            val keyInMemory = encryptionRepository.isNotebookUnlocked(notebookPath)

            println("📊 Состояние перед разблокировкой:")
            println("   - Зашифрован: $currentlyEncrypted")
            println("   - Разблокирован в prefs: $currentlyUnlocked")
            println("   - Ключ в памяти: $keyInMemory")

            encryptionRepository.debugKeyStoreState(notebookPath)

            biometricRepository.authenticate(activity).map {
                println("✅ Биометрия успешна")
//                encryptionRepository.decryptNotebook(notebookPath) // ключ в памяти
//                securityPreferences.setNotebookUnlocked(notebookPath, true) // состояние в prefs
                val decryptResult = encryptionRepository.decryptNotebook(notebookPath)
                println("✅ Результат дешифровки: ${decryptResult.isSuccess}")
                securityPreferences.setNotebookUnlocked(notebookPath, true)

                // Проверим состояние после разблокировки
                val afterUnlocked = securityPreferences.isNotebookUnlocked(notebookPath)
                val afterKeyInMemory = encryptionRepository.isNotebookUnlocked(notebookPath)

                println("📊 Состояние после разблокировки:")
                println("   - Разблокирован в prefs: $afterUnlocked")
                println("   - Ключ в памяти: $afterKeyInMemory")

                println("🎯 Разблокировка завершена")
            }
        } catch (e: Exception) {
            println("❌ Ошибка разблокировки: ${e.message}")
            e.printStackTrace()

            Result.failure(Exception("Ошибка разблокировки: ${e.message}"))
        }
    }
}