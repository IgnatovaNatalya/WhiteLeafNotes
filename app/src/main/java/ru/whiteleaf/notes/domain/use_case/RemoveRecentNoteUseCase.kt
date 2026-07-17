package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class RemoveRecentNoteUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(note: Note) {
        preferencesRepository.removeRecentNote(note.id, note.notebookPath)
    }

}