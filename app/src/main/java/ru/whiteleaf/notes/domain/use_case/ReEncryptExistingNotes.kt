package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotesRepository

class ReEncryptExistingNotes(private val repository: NotesRepository) { //, keyAlias: String) {
    suspend operator fun invoke(notebookPath: String) {
        // Получаем заметки из книжки
        val notes = repository.getNotes(notebookPath)

        // Пересохраняем каждую заметку - они автоматически зашифруются
        // благодаря логике в FileNoteDataSource
        notes.forEach { note ->  repository.saveNote(note) }
    }
}