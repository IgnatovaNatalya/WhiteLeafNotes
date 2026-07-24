package ru.whiteleaf.notes.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.whiteleaf.notes.data.model.RecentNote

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
        prefs.edit { putInt(key, scrollY) }
    }

    override fun getNoteScrollPosition(noteId: String, notebookPath: String): Int? {
        val key = makeScrollKey(noteId, notebookPath)
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    private fun makeScrollKey(noteId: String, notebookPath: String): String {
        return "$KEY_NOTE_SCROLL_POSITION_PREFIX${notebookPath}_${noteId}"
    }

    private val noteType = object : TypeToken<List<RecentNote>>() {}.type

     fun saveRecentNoteNew(note: Note) { //просто сохраняем сверху
        val entry = RecentNote.fromNote(note)

        val currentList = getRecentNotes().toMutableList()
        currentList.add(0, entry)

        if (currentList.size > MAX_SIZE) {
            currentList.removeAt(currentList.size - 1)
        }
        val json = gson.toJson(currentList)
        prefs.edit { putString(KEY_RECENT_NOTES, json) }
    }


    override fun saveRecentNote(note: Note) {

        val entry = RecentNote.fromNote(note)
        println("DEBUG: PreferencesRepositoryImpl saveNoteToRecent: entry: ${entry.printDebug()}")

        // Получаем текущий список
        val currentList = getRecentNotes().toMutableList()
        val normalizedPath = note.notebookPath ?: ""

        // Проверяем, есть ли уже такая заметка
        val existingIndex =
            currentList.indexOfFirst { it.id == note.id && it.notebookPath == normalizedPath }

        when {
            // Если заметка уже первая - ничего не делаем
            existingIndex == 0 -> return

            // Если заметка есть в списке - перемещаем в начало
            existingIndex > 0 -> {
                currentList.removeAt(existingIndex)
                currentList.add(0, entry)
            }

            // Если заметки нет в списке
            else -> {
                // Добавляем в начало
                currentList.add(0, entry)

                // Если больше MAX_SIZE, удаляем последний
                if (currentList.size > MAX_SIZE) {
                    currentList.removeAt(currentList.size - 1)
                }
            }
        }

        // Сохраняем обновленный список
        val json = gson.toJson(currentList)
        prefs.edit { putString(KEY_RECENT_NOTES, json) }
    }

    override fun getRecentNotes(): List<RecentNote> {
        val json = prefs.getString(KEY_RECENT_NOTES, null) ?: return emptyList()
        return try {
            gson.fromJson(json, noteType) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }


    override fun updateRecentEntry(
        oldNote: Note,
        newNote: Note,
    ): Boolean {

        println("DEBUG: PreferencesRepositoryImpl updateRecentEntry: old: ${oldNote.printDebugIdTitlePath()}, new: ${newNote.printDebugIdTitlePath()}")

        val currentList = getRecentNotes().toMutableList()

        val index =
            currentList.indexOfFirst {
                it.id == oldNote.id && it.notebookPath == (oldNote.notebookPath ?: "")
            }

        return if (index != -1) {
            val updatedEntry = RecentNote.fromNote(newNote)
            currentList[index] = updatedEntry

            val json = gson.toJson(currentList)
            prefs.edit { putString(KEY_RECENT_NOTES, json) }
            true
        } else false
    }


    override fun updateRecentNoteNotebookPath(
        noteId: String,
        oldNotebookPath: String?,
        newNotebookPath: String?
    ): Boolean {
        val currentList = getRecentNotes().toMutableList()

        val normalizedOldPath = oldNotebookPath ?: ""
        val normalizedNewPath = newNotebookPath ?: ""

        val index =
            currentList.indexOfFirst { it.id == noteId && it.notebookPath == normalizedOldPath }

        return if (index != -1) {
            val oldEntry = currentList[index]
            val updatedEntry = oldEntry.copy(notebookPath = normalizedNewPath)

            currentList[index] = updatedEntry

            val json = gson.toJson(currentList)
            prefs.edit { putString(KEY_RECENT_NOTES, json) }
            true
        } else {
            false
        }
    }

    override fun removeRecentNotesByNotebookPath(notebookPath: String) {
        println("DEBUG: PreferencesRepositoryImpl removeRecentNotesByNotebookPath $notebookPath")
        val currentList = getRecentNotes().toMutableList()
        val removed = currentList.removeAll { it.notebookPath == notebookPath }

        if (removed) {
            val json = gson.toJson(currentList)
            prefs.edit { putString(KEY_RECENT_NOTES, json) }
        }
    }

    override fun removeRecentNote(noteId: String, notebookPath: String?) {
        println("DEBUG: PreferencesRepositoryImpl removeRecentNote $noteId")
        val currentList = getRecentNotes().toMutableList()
        val normalizedPath = notebookPath ?: ""

        val removed = currentList.removeAll { it.notebookPath == normalizedPath && it.id == noteId }

        if (removed) {
            val json = gson.toJson(currentList)
            prefs.edit { putString(KEY_RECENT_NOTES, json) }
        }
    }
}