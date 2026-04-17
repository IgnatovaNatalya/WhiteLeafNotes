package ru.whiteleaf.notes.domain.repository

interface PreferencesRepository {
    fun saveLastOpenedNotebook(notebookPath: String)
    fun saveViewMode(notebookPath: String, isPlannerMode: Boolean)
    fun getViewMode(notebookPath: String, defaultIsPlanner: Boolean = false): Boolean
}