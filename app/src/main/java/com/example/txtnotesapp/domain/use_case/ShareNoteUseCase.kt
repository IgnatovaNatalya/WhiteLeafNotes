package com.example.txtnotesapp.domain.use_case

import android.net.Uri
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NotesRepository

class ShareNoteUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note): Uri? {
        return repository.shareNoteFile(note)
    }
}