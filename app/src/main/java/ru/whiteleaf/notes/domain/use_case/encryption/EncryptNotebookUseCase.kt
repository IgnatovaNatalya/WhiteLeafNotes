package ru.whiteleaf.notes.domain.use_case.encryption

import ru.whiteleaf.notes.domain.repository.NotesRepository

class EncryptNotebookUseCase(private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String) {
        repository.encryptAllNotes(notebookPath)
    }
}