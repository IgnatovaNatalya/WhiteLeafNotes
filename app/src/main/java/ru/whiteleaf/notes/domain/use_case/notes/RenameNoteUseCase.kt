package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class RenameNoteUseCase(
    private val repository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(oldNote: Note, newName: String): Note {

        val renamedNote = repository.renameNote(oldNote, newName)
        preferencesRepository.updateRecentEntry(oldNote, renamedNote)
        return renamedNote
    }
}