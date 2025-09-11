package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.common.AppConstants.FILE_NAME_PREFIX
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class CreateNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String? = null): Note {
        val timestamp = System.currentTimeMillis()
        val noteId =  "$FILE_NAME_PREFIX${timestamp}"

        val note = Note(
            id = noteId,
            title = "",
            content = "",
            modifiedAt = timestamp,
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}