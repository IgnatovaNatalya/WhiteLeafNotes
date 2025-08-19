package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NoteRepository

class GetNotes (private val repository: NoteRepository){
    suspend operator fun invoke(notebookPath: String?): List<Note> {
        return repository.getNotes(notebookPath)
    }
}