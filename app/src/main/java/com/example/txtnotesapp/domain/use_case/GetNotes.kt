package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class GetNotes(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String?): List<Note> {
        return repository.getNotes(notebookPath)
    }
}