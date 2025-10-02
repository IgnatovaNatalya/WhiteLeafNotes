package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.common.utils.FileUtils.generateNoteId
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class CreateNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String? = null): Note {

        val note = Note(
            id = generateNoteId(),
            title = "",
            content = " ",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}