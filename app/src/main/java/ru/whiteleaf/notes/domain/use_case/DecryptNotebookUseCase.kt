package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotesRepository

class DecryptNotebookUseCase (private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String) {
        repository.decryptAllNotes(notebookPath)
    }
}