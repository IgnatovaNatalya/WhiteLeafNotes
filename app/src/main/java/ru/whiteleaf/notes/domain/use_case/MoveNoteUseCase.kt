package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class MoveNoteUseCase(
    private val repository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(note: Note, targetNotebookPath: String?) {
        preferencesRepository.updateRecentNoteNotebookPath(
            note.id,
            note.notebookPath,
            targetNotebookPath
        )
        repository.moveNote(note, targetNotebookPath)
    }
}