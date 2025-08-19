package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NoteRepository

class CreateNote(private val repository: NoteRepository) {
    suspend operator fun invoke(notebookPath: String? = null): Note {
        val timestamp = System.currentTimeMillis()
        val noteId = "note_${timestamp}"

        val note = Note(
            title = noteId,
            content = "",
            createdAt = timestamp,
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}