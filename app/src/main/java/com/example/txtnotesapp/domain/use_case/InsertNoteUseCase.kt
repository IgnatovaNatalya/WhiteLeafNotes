package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.common.AppConstants.FILE_NAME_PREFIX
import com.example.txtnotesapp.common.utils.FileUtils.sanitizeFileName
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class InsertNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(
        noteTitle: String,
        noteContent: String,
        notebookPath: String? = null
    ): Note {
        val timestamp = System.currentTimeMillis()
        val noteId = "$FILE_NAME_PREFIX${timestamp}"
        //val clearTitle = noteTitle.replace(Regex("[^a-zA-Z0-9_\\- ,]"), "")
        val clearTitle = sanitizeFileName(noteTitle)

        val note = Note(
            id = if (clearTitle == "") noteId else clearTitle,
            title = clearTitle,
            content = noteContent,
            modifiedAt = timestamp,
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}