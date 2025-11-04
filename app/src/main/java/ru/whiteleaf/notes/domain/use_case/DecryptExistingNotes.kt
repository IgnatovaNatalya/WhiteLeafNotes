package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotesRepository

class DecryptExistingNotes(private val repository: NotesRepository) {

    suspend operator fun invoke(notebookPath: String): Boolean {
        return try {
            // пересохраняем заметки, они расшифруются благодяря логике FileNoteDatasource
            val notes = repository.getNotes(notebookPath)
            notes.forEach { note -> repository.saveNote(note) }
            true
        } catch (e: SecurityException) {
            // Пробрасываем SecurityException для обработки биометрии
            throw e
        } catch (e: Exception) {
            false
        }
    }
}