package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class GetNotebooksUseCase(private val repository: NotebookRepository) {
    suspend operator fun invoke(): List<Notebook> {
        return repository.getNotebooks()
    }
}