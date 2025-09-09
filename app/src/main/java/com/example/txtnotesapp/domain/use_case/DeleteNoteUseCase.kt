package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class DeleteNoteUseCase(private val repository: NotesRepository)  {
    suspend operator fun invoke(note: Note) {
        repository.deleteNote(note)
    }
}