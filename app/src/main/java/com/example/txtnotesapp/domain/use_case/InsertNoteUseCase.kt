package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.common.utils.FileUtils.generateNoteId
import com.example.txtnotesapp.common.utils.FileUtils.sanitizeFileName
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

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