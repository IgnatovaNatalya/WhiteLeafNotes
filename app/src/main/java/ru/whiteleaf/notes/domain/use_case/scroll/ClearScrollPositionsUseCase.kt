package ru.whiteleaf.notes.domain.use_case.scroll

import android.content.SharedPreferences
import androidx.core.content.edit
import ru.whiteleaf.notes.domain.repository.NotesRepository

const val  KEY_NOTE_SCROLL_POSITION_PREFIX = "note_scroll_pos_"

class ClearScrollPositionsUseCase(
    private val prefs: SharedPreferences,
    private val repository: NotesRepository
) {

    /**
     * Удаляет все записи позиций скролла, которые:
     * 1) имеют значение 0
     * 2) ссылаются на несуществующие заметки
     */
    suspend operator fun invoke() {
        val allEntries = prefs.all
        val keysToRemove = mutableListOf<String>()

        for ((key, value) in allEntries) {
            // Пропускаем ключи, не относящиеся к позициям скролла
            if (!key.startsWith(KEY_NOTE_SCROLL_POSITION_PREFIX)) continue
            if (value !is Int) continue

            val scrollPosition = value

            // Удаляем записи с нулевой позицией
            if (scrollPosition == 0) {
                keysToRemove.add(key)
                continue
            }

            // Разбираем ключ для извлечения notebookPath и noteId
            val suffix = key.substring(KEY_NOTE_SCROLL_POSITION_PREFIX.length)
            val underscoreIndex = suffix.indexOf('_')

            // Если разделитель отсутствует — ключ некорректен, удаляем
            if (underscoreIndex == -1) {
                keysToRemove.add(key)
                continue
            }

            val notebookPath = suffix.substring(0, underscoreIndex)
            val noteId = suffix.substring(underscoreIndex + 1)

            // Проверяем существование заметки
            val noteExists = repository.existsNote(notebookPath, noteId)
            if (!noteExists) {
                keysToRemove.add(key)
            }
        }

        // Удаляем все накопленные ключи одной транзакцией
        if (keysToRemove.isNotEmpty()) {
            prefs.edit {
                keysToRemove.forEach { remove(it) }
            }//.apply()
        }
    }
}