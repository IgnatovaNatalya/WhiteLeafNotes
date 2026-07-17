package ru.whiteleaf.notes.domain.use_case

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
        preferencesRepository.updateRecentNoteTitle(note.title, clearTitle, note.notebookPath)
        return repository.renameNote(note, clearTitle)
    }
}