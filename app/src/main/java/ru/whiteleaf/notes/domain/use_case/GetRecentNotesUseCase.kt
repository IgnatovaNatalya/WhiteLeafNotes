package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class GetRecentNotesUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(): List<RecentNote> {
        return preferencesRepository.getRecentNotes()
    }
}