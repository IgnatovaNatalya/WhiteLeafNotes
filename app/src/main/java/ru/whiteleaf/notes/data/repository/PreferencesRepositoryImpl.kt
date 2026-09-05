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
        private const val KEY_PIN_PREFIX = "notebook_pinned_"
        private const val KEY_NOTE_SCROLL_POSITION_PREFIX = "note_scroll_pos_"
        private const val KEY_RECENT_NOTES = "recent_notes"
        private const val MAX_SIZE = 5
    }

    //LAST OPENED

    override fun saveLastOpenedNotebook(notebookPath: String) {
        prefs.edit {
            putString(KEY_LAST_OPENED_NOTEBOOK, notebookPath)
        }
    }

    override fun getLastOpenedNotebook(): String? = prefs.getString(KEY_LAST_OPENED_NOTEBOOK, null)

    //VIEW MODE

    override fun saveViewMode(notebookPath: String, isPlannerMode: Boolean) {
        val key = "$KEY_VIEW_MODE_PREFIX$notebookPath"
        prefs.edit { putBoolean(key, isPlannerMode) }
    }

    override fun getViewMode(notebookPath: String, defaultIsPlanner: Boolean): Boolean {
        val key = "$KEY_VIEW_MODE_PREFIX$notebookPath"
        return prefs.getBoolean(key, defaultIsPlanner)
    }

    //PINED NOTEBOOKS
    override fun pinNotebook(notebookPath: String) {
        val key = "$KEY_PIN_PREFIX$notebookPath"
        prefs.edit { putBoolean(key, true) }
    }

    override fun unpinNotebook(notebookPath: String) {
        val key = "$KEY_PIN_PREFIX$notebookPath"
        prefs.edit { remove(key) }
    }

    override fun isNotebookPinned(notebookPath: String): Boolean {
        val key = "$KEY_PIN_PREFIX$notebookPath"
        return prefs.contains(key)
    }


    //SCROLL POS
    override fun saveNoteScrollPosition(noteId: String, notebookPath: String, scrollY: Int) {
        val key = makeScrollKey(noteId, notebookPath)
        if (scrollY == 0 && !prefs.contains(key)) return
        println("DEBUG: PreferencesRepositoryImpl: saveNoteScrollPosition Scroll noteId=$noteId, notebookPath=$notebookPath, pos=$scrollY")
        prefs.edit { putInt(key, scrollY) }
    }

    override fun getNoteScrollPosition(noteId: String, notebookPath: String): Int? {
        val key = makeScrollKey(noteId, notebookPath)
        return if (prefs.contains(key)) prefs.getInt(key, 0) else null
    }

    override fun clearNoteScrollPosition(noteId: String, notebookPath: String) {
        val key = makeScrollKey(noteId, notebookPath)
        prefs.edit { remove(key) }
    }

    override fun updateNoteScrollPosition(oldNote: Note, newNote: Note) {
        val oldKey = makeScrollKey(oldNote.id, oldNote.notebookPath ?: "")
        val newKey = makeScrollKey(newNote.id, newNote.notebookPath ?: "")
        val pos = if (prefs.contains(oldKey)) prefs.getInt(oldKey, 0) else null
        if (pos != null) {
            prefs.edit {
                remove(oldKey)
                putInt(newKey, pos)
            }
        }
    }

    private fun makeScrollKey(noteId: String, notebookPath: String): String {
        return "$KEY_NOTE_SCROLL_POSITION_PREFIX${notebookPath}_${noteId}"
    }


    //NOTEBOOK Changed
    override fun updateNotebookPreferences(oldPath: String, newPath: String) {
        if (oldPath == newPath) return

        val allEntries = prefs.all // Map<String, *>
        prefs.edit {
            allEntries.forEach { (key, value) ->
                when {
                    // Ключ режима просмотра для этого блокнота
                    key == "$KEY_VIEW_MODE_PREFIX$oldPath" -> {
                        val newKey = "$KEY_VIEW_MODE_PREFIX$newPath"
                        putBoolean(newKey, value as Boolean)
                        remove(key)
                    }

                    // Ключ закрепеленности
                    key == "$KEY_PIN_PREFIX$oldPath" -> {
                        val newKey = "$KEY_PIN_PREFIX$newPath"
                        putBoolean(newKey, value as Boolean)
                        remove(key)
                    }
                    // Ключ позиции скролла для заметок в этом блокноте

                    key.startsWith("$KEY_NOTE_SCROLL_POSITION_PREFIX$oldPath" + "_") -> {
                        // Заменяем старый путь на новый в ключе
                        println("DEBUG: PreferencesRepositoryImpl: updateNotebookPreferences Scroll oldPath=$oldPath, newPath=$newPath, pos=$value")
                        val suffix =
                            key.removePrefix("$KEY_NOTE_SCROLL_POSITION_PREFIX$oldPath" + "_")
                        val newKey = "$KEY_NOTE_SCROLL_POSITION_PREFIX$newPath" + "_$suffix"
                        putInt(newKey, value as Int)
                        remove(key)
                    }
                }
            }

            val lastPath = prefs.getString(KEY_LAST_OPENED_NOTEBOOK, null)
            if (lastPath == oldPath) {
                putString(KEY_LAST_OPENED_NOTEBOOK, newPath)
            }
        }
    }


    override fun removeNotebookPreferences(notebookPath: String) {
        val allEntries = prefs.all
        prefs.edit {
            allEntries.forEach { (key, _) ->
                when {
                    // Удаляем ключ режима просмотра
                    key == "$KEY_VIEW_MODE_PREFIX$notebookPath" -> remove(key)
                    // Удаляем ключ закрепления
                    key == "$KEY_PIN_PREFIX$notebookPath" -> remove(key)
                    // Удаляем все ключи позиций скролла для этого блокнота
                    key.startsWith("$KEY_NOTE_SCROLL_POSITION_PREFIX$notebookPath" + "_") -> remove(
                        key
                    )
                }
            }
        }
    }

    //RECENT
    private val noteType = object : TypeToken<List<RecentNote>>() {}.type

    //Recent - notes
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
        } catch (_: Exception) {
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

    //Recent - notebooks
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

    override fun updateNotebookPathInRecent(oldNotebookPath: String?, newNotebookPath: String?) {
        println("DEBUG: PreferencesRepositoryImpl updateNotebookPathInRecent $oldNotebookPath to $newNotebookPath")
        val currentList = getRecentNotes()
        if (currentList.isEmpty()) return

        val updatedList = currentList.map { recent ->
            if (recent.notebookPath == oldNotebookPath) {
                recent.copy(notebookPath = newNotebookPath)
            } else {
                recent
            }
        }

        // Проверяем, были ли изменения (можно также сравнить списки, но проще через флаг)
        val hasChanges = currentList.zip(updatedList).any { (old, new) -> old != new }

        if (hasChanges) {
            val json = gson.toJson(updatedList)
            prefs.edit { putString(KEY_RECENT_NOTES, json) }
        }
    }
}