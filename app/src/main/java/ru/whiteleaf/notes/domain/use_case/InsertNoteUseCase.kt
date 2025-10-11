package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.common.utils.FileUtils.generateNoteId
import ru.whiteleaf.notes.common.utils.FileUtils.sanitizeFileName
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class InsertNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(
        noteTitle: String?,
        noteContent: String,
        notebookPath: String
    ): Note {

        val baseId =
            sanitizeFileName(noteTitle?.takeIf { it.isNotBlank() } ?: generateNoteId())

        var counter = 1
        var noteId = baseId

        while (repository.existsNote(notebookPath, noteId)) {
            noteId = "${baseId}_${counter++}"
        }

        val note = Note(
            id = noteId,
            title = noteId,
            content = noteContent,
            modifiedAt = System.currentTimeMillis(),
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}