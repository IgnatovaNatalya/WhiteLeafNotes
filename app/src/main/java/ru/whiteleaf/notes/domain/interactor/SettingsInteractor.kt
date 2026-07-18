package ru.whiteleaf.notes.domain.interactor

import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class SettingsInteractor(private val preferencesRepository: PreferencesRepository) {
    fun saveLastOpenedNotebook(notebookPath: String) {
        preferencesRepository.saveLastOpenedNotebook(notebookPath)
    }

    fun saveViewMode(notebookPath: String, isPlannerMode: Boolean) {
        preferencesRepository.saveViewMode(notebookPath, isPlannerMode)
    }

    fun getViewMode(notebookPath: String): Boolean {
        return preferencesRepository.getViewMode(notebookPath, false)
    }

    fun saveNoteScrollPosition(noteId: String, notebookPath: String, scrollPosition: Int) {
        preferencesRepository.saveNoteScrollPosition(noteId, notebookPath, scrollPosition)
    }

    fun getNoteScrollPosition(noteId: String, notebookPath: String): Int? {
        return preferencesRepository.getNoteScrollPosition(noteId, notebookPath)
    }

}