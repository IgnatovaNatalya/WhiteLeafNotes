package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class GetRecentNotesUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke(): List<RecentNote> {
        return preferencesRepository.getRecentNoteDataList()
            .map { recentEntry ->
                RecentNote(
                    id = recentEntry.id,
                    title = notesRepository.getRecentNoteTitle(
                        recentEntry.notebookPath,
                        recentEntry.id,
                    ),
                    notebookPath = recentEntry.notebookPath,
                    recentDate = recentEntry.recentDate
                )
            }
    }
}