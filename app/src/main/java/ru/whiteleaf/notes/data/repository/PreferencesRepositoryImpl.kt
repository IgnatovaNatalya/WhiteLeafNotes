package ru.whiteleaf.notes.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class PreferencesRepositoryImpl(private val prefs: SharedPreferences) : PreferencesRepository {
    companion object {
        private const val KEY_VIEW_MODE_PREFIX = "view_mode_planner_"
        private const val KEY_LAST_OPENED_NOTEBOOK = "last_notebook_path"
        private const val KEY_NOTE_SCROLL_POSITION_PREFIX = "note_scroll_pos_"
    }

    override fun saveLastOpenedNotebook(notebookPath: String) {
        prefs.edit {
            putString(KEY_LAST_OPENED_NOTEBOOK, notebookPath)
        }
    }

    override fun getLastOpenedNotebook(): String? = prefs.getString(KEY_LAST_OPENED_NOTEBOOK, null)

    override fun saveViewMode(notebookPath: String, isPlannerMode: Boolean) {
        val key = "$KEY_VIEW_MODE_PREFIX$notebookPath"
        prefs.edit { putBoolean(key, isPlannerMode) }
    }

    override fun getViewMode(notebookPath: String, defaultIsPlanner: Boolean): Boolean {
        val key = "$KEY_VIEW_MODE_PREFIX$notebookPath"
        return prefs.getBoolean(key, defaultIsPlanner)
    }

    override fun saveNoteScrollPosition(noteId: String, notebookPath: String, scrollY: Int) {
        val key = makeScrollKey(noteId, notebookPath)
        prefs.edit().putInt(key, scrollY).apply()
    }

    override fun getNoteScrollPosition(noteId: String, notebookPath: String): Int? {
        val key = makeScrollKey(noteId, notebookPath)
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    private fun makeScrollKey(noteId: String, notebookPath: String): String {
        return "$KEY_NOTE_SCROLL_POSITION_PREFIX${notebookPath}_${noteId}"
    }
}