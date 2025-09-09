package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class SaveNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note) {
        repository.saveNote(note)
    }
}