package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotesRepository

class DecryptExistingNotes(private val repository: NotesRepository) {

    suspend operator fun invoke(notebookPath: String,) {
        // пересохраняем заметки, они расшифруются благодяря логике FileNoteDatasource
        val notes = repository.getNotes(notebookPath)
        notes.forEach { note -> repository.saveNote(note)}
    }
}