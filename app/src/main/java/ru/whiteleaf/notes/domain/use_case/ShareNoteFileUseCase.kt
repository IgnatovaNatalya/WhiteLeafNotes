package ru.whiteleaf.notes.domain.use_case

import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class ShareNoteFileUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(note: Note): Uri? {
        return repository.shareNoteFile(note)
    }
}