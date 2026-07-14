package ru.whiteleaf.notes.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.whiteleaf.notes.data.model.RecentNoteData

class PreferencesRepositoryImpl(private val prefs: SharedPreferences, private val gson: Gson) :
    PreferencesRepository {
    companion object {
        private const val KEY_VIEW_MODE_PREFIX = "view_mode_planner_"
        private const val KEY_LAST_OPENED_NOTEBOOK = "last_notebook_path"
        private const val KEY_NOTE_SCROLL_POSITION_PREFIX = "note_scroll_pos_"
        private const val KEY_RECENT_NOTES = "recent_notes"
        private const val MAX_SIZE = 5
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

    private val noteType = object : TypeToken<List<RecentNoteData>>() {}.type

    override fun saveRecentNote(note: Note) {
        // Получаем текущий список
        val currentList = getRecentNoteDataList().toMutableList()

        // Проверяем, есть ли уже такая заметка
        val existingIndex = currentList.indexOfFirst { it.id == note.id }

        when {
            // Если заметка уже первая - ничего не делаем
            existingIndex == 0 -> return

            // Если заметка есть в списке - перемещаем в начало
            existingIndex > 0 -> {
                currentList.removeAt(existingIndex)
                currentList.add(0, RecentNoteData.fromNote(note))
            }

            // Если заметки нет в списке
            else -> {
                // Добавляем в начало
                currentList.add(0, RecentNoteData.fromNote(note))

                // Если больше MAX_SIZE, удаляем последний
                if (currentList.size > MAX_SIZE) {
                    currentList.removeAt(currentList.size - 1)
                }
            }
        }

        // Сохраняем обновленный список
        val json = gson.toJson(currentList)
        prefs.edit().putString(KEY_RECENT_NOTES, json).apply()
    }

    override fun getRecentNoteDataList(): List<RecentNoteData> {
        val json = prefs.getString(KEY_RECENT_NOTES, null) ?: return emptyList()
        return try {
            gson.fromJson(json, noteType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }


    private fun makeScrollKey(noteId: String, notebookPath: String): String {
        return "$KEY_NOTE_SCROLL_POSITION_PREFIX${notebookPath}_${noteId}"
    }
}