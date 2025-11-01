package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.data.config.NotebookConfigManager
import ru.whiteleaf.notes.domain.repository.NotesRepository

class ReEncryptExistingNotes(
    private val repository: NotesRepository,
    private val configManager: NotebookConfigManager
) {

    suspend operator fun invoke(notebookPath: String) {
        try {
            println("🔄 Starting re-encryption for notebook: $notebookPath")

            // Временно отключаем защиту для перешифровки
            val wasProtected = configManager.isNotebookProtected(notebookPath)
            val keyAlias = configManager.getKeyAliasForNotebook(notebookPath)

            if (wasProtected && keyAlias != null) {
                println("🔄 Temporarily disabling protection for re-encryption")
                configManager.setNotebookUnprotected(notebookPath)
            }

            // Получаем заметки (теперь они будут читаться без биометрии)
            val notes = repository.getNotes(notebookPath)
            println("🔄 Found ${notes.size} notes to re-encrypt")

            // Восстанавливаем защиту
            if (wasProtected && keyAlias != null) {
                println("🔄 Re-enabling protection")
                configManager.setNotebookProtected(notebookPath, keyAlias)
            }

            // Пересохраняем заметки - они зашифруются автоматически
            var successCount = 0
            notes.forEach { note ->
                try {
                    repository.saveNote(note)
                    successCount++
                    println("✅ Re-encrypted note: ${note.id}")
                } catch (e: Exception) {
                    println("❌ Failed to re-encrypt note ${note.id}: ${e.message}")
                }
            }

            println("🔄 Re-encryption completed: $successCount/${notes.size} success")

        } catch (e: Exception) {
            println("❌ Error in reencryptExistingNotes: ${e.message}")
            // Не бросаем исключение - книга считается защищенной для новых заметок
        }
    }


//
//    suspend operator fun invoke(notebookPath: String) {
//        try {
//            println("DEBUG: Starting re-encryption for notebook: $notebookPath")
//
//            // Получаем заметки из книжки
//            val notes = repository.getNotes(notebookPath)
//            println("DEBUG: Found ${notes.size} notes to re-encrypt")
//            var successCount = 0
//            var skipCount = 0
//
//            notes.forEach { note ->
//                try {
//                    println("DEBUG: Processing note: ${note.id}")
//
//                    repository.saveNote(note)
//
//                    successCount++
//                    println("DEBUG: Successfully re-encrypted note: ${note.id}")
//
//                } catch (e: Exception) {
//                    skipCount++
//                    println("DEBUG: Failed to re-encrypt note ${note.id}: ${e.message}")
//                }
//            }
//        } catch (e: Exception) {
//            println("DEBUG: Error in reencryptExistingNotes: ${e.message}")
//        }
//    }
}