package ru.whiteleaf.notes.domain.use_case.notes

import ru.whiteleaf.notes.common.utils.FileUtils.sanitizeFileName
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class RenameNoteUseCase(
    private val repository: NotesRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(note: Note, newName: String): String {
        val clearTitle = sanitizeFileName(newName)
        if (!preferencesRepository.updateRecentNoteTitle(note.title, clearTitle, note.notebookPath))
            preferencesRepository.saveNoteToRecent(note.copy(id = newName))
        return repository.renameNote(note, clearTitle)
    }
}