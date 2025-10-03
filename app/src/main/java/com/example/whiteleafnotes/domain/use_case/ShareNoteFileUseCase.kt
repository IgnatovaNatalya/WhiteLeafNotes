package com.example.whiteleafnotes.domain.use_case

import android.net.Uri
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.repository.NotesRepository

class ShareNoteFileUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note): Uri? {
        return repository.shareNoteFile(note)
    }
}