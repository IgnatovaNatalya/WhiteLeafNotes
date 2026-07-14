package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class GetRecentNotesUseCase(
    private val preferencesRepository: PreferencesRepository,
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke():List<RecentNote> {
//        val recentNotes = mutableListOf<RecentNote>()
//        val recentNoteDataList = preferencesRepository.getRecentNoteDataList()
//        for (recentEntry in recentNoteDataList) {
//            val recentNote = notesRepository.getRecentNoteInNotebookById(recentEntry.notebookPath, recentEntry.id)
//            recentNotes.add(recentNote)
//        }
//        return recentNotes.toList()

        return preferencesRepository.getRecentNoteDataList()
            .map { recentEntry ->
                notesRepository.getRecentNoteInNotebookById(recentEntry.notebookPath, recentEntry.id)
            }
    }
}