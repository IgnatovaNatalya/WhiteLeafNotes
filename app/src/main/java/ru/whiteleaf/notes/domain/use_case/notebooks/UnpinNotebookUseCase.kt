package ru.whiteleaf.notes.domain.use_case.notebooks

import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class UnpinNotebookUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(notebookPath: String) {
        preferencesRepository.unpinNotebook(notebookPath)
    }
}