package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.common.utils.FileUtils.sanitizeFileName
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class RenameNoteUseCase(
    private val repository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(oldNote: Note, newNote: String): String {
        val clearTitle = sanitizeFileName(newNote)
        if (!preferencesRepository.updateRecentNoteTitle(oldNote, clearTitle, oldNote.notebookPath))
            preferencesRepository.saveRecentNote(oldNote.copy(id = clearTitle, title = clearTitle))
        return repository.renameNote(oldNote, clearTitle)
    }
}