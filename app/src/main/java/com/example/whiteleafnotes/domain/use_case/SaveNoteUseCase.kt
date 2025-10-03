package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

class SaveNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note) {
        repository.saveNote(note)
    }
}