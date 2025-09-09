package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class RenameNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, newName: String) {
        repository.renameNote(note, newName)
    }
}