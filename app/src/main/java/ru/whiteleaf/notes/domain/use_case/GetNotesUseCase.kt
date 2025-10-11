package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class GetNotesUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String?): List<Note> {
        return repository.getNotes(notebookPath)
    }
}