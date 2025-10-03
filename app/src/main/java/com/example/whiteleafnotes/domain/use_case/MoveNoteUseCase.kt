package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

class MoveNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, targetNotebookPath: String?) {
        repository.moveNote(note, targetNotebookPath)
    }
}