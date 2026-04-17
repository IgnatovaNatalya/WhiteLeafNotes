package ru.whiteleaf.notes.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class PreferencesRepositoryImpl(private val prefs: SharedPreferences) : PreferencesRepository {
    companion object {
        private const val KEY_VIEW_MODE_PREFIX = "view_mode_planner_"
        private const val KEY_LAST_OPENED_NOTEBOOK = "last_notebook_path"
    }

    override fun saveLastOpenedNotebook(notebookPath: String) {
        prefs.edit {
            putString(KEY_LAST_OPENED_NOTEBOOK, notebookPath)
        }
    }

    override fun saveViewMode(notebookPath: String, isPlannerMode: Boolean) {
        val key = "$KEY_VIEW_MODE_PREFIX$notebookPath"
        prefs.edit { putBoolean(key, isPlannerMode) }
    }

    override fun getViewMode(notebookPath: String, defaultIsPlanner: Boolean): Boolean {
        val key = "$KEY_VIEW_MODE_PREFIX$notebookPath"
        return prefs.getBoolean(key, defaultIsPlanner)
    }
}