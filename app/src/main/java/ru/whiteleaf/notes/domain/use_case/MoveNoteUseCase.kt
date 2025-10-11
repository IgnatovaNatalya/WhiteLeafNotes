package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class MoveNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, targetNotebookPath: String?) {
        repository.moveNote(note, targetNotebookPath)
    }
}