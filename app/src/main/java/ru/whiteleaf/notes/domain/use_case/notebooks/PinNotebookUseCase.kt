package ru.whiteleaf.notes.domain.use_case.notebooks

import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class PinNotebookUseCase(
    private val preferencesRepository: PreferencesRepository
) {
    operator fun invoke(notebookPath: String) {
        preferencesRepository.pinNotebook(notebookPath)
    }
}