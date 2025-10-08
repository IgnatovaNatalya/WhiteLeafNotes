package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.common.utils.FileUtils.generateNoteId
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

class CreateNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String? = null): Note {

        val note = Note(
            id = generateNoteId(),
            title = "",
            content = "",
            modifiedAt = System.currentTimeMillis(),
            notebookPath = notebookPath
        )

        repository.saveNote(note)
        return note
    }
}