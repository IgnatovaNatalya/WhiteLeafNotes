package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class SaveNoteContentUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note) {
        repository.saveNote(note)
    }
}