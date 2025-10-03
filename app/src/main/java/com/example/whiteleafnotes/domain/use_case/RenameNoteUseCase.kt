package com.example.whiteleafnotes.domain.use_case

import com.example.whiteleafnotes.common.utils.FileUtils.sanitizeFileName
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

class RenameNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note, newName: String):String {
        val clearTitle = sanitizeFileName(newName)
        //repository.renameNote(note, newName)
        return repository.renameNote(note, clearTitle)
    }
}