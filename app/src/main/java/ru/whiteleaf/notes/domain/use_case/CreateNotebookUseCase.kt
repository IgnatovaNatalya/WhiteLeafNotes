package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class CreateNotebookUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(name: String): Notebook {
        return repository.createNotebook(name)
    }
}