package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences

class DeleteNotebookByPathUseCase(
    private val notebookRepository: NotebookRepository,
    private val encryptionRepository: EncryptionRepository,
    private val securityPreferences: SecurityPreferences
) {
    suspend operator fun invoke(notebookPath: String) {
        val notebook = notebookRepository.getNotebookByPath(notebookPath)
        if (notebook != null) {
            println("🗑️ НАЧАЛО УДАЛЕНИЯ БЛОКНОТА: $notebookPath")
            println("🔑 Очищаем ключи...")
            encryptionRepository.clearNotebookKeys(notebookPath)

            println("📊 Сбрасываем состояние безопасности...")
            securityPreferences.setNotebookEncrypted(notebookPath, false)
            securityPreferences.setNotebookUnlocked(notebookPath, false)

            println("📁 Удаляем файлы блокнота...")
            notebookRepository.deleteNotebook(notebook)

            println("✅ Блокнот полностью удален")
        }
    }
}