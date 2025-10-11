package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.common.utils.FileUtils.sanitizeFileName
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class RenameNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, newName: String):String {
        val clearTitle = sanitizeFileName(newName)
        //repository.renameNote(note, newName)
        return repository.renameNote(note, clearTitle)
    }
}