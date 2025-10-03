package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

class GetNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: String, notebookPath: String?): Note? {
        return repository.getNotes(notebookPath).find { it.id == noteId }
    }
}