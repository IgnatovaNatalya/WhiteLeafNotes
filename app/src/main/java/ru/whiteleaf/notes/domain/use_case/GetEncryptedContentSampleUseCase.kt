package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.NotesRepository

class GetEncryptedContentSampleUseCase (private val repository: NotesRepository) {
    suspend operator fun invoke(notebookPath: String):String? {
        return repository.getEncryptedContentSample(notebookPath)
    }
}

