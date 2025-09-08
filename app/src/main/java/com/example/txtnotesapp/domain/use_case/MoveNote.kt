package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class MoveNote(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, targetNotebookPath: String?) {
        repository.moveNote(note, targetNotebookPath)
    }
}