package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NoteRepository

class GetNote(private val repository: NoteRepository) {
    suspend operator fun invoke(noteId: String, notebookPath: String?): Note? {
        return repository.getNotes(notebookPath).find { it.title == noteId }
    }
}