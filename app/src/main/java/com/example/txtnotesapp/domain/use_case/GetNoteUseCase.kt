package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class GetNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(noteId: String, notebookPath: String?): Note? {
        return repository.getNotes(notebookPath).find { it.id == noteId }
    }
}