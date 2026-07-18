package ru.whiteleaf.notes.domain.use_case.recent

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class SaveRecentNoteUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(note: Note) {
        preferencesRepository.saveRecentNote(note)
    }

}