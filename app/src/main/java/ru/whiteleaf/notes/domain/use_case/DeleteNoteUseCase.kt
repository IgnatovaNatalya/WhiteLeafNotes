package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class DeleteNoteUseCase(private val repository: NotesRepository)  {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}