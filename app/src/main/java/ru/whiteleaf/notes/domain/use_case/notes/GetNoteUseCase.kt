package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class GetNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: String, notebookPath: String?): Note {
        // return repository.getNotes(notebookPath).find { it.id == noteId }
        return repository.getNote(noteId, notebookPath)
    }
}