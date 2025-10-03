package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.common.utils.FileUtils.generateNoteId
import com.example.whiteleafnotes.common.utils.FileUtils.sanitizeFileName
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

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