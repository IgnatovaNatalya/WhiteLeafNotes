package ru.whiteleaf.notes.common.utils

import ru.whiteleaf.notes.presentation.note_edit.adapter.ImageBlockViewHolder
import ru.whiteleaf.notes.presentation.note_edit.adapter.TableBlockViewHolder
import ru.whiteleaf.notes.presentation.note_edit.adapter.TextBlockViewHolder
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.domain.model.ContentBlock
import ru.whiteleaf.notes.presentation.note_edit.adapter.ContentBlockAdapter

/**
 * Утилита для синхронизации текста из ViewHolder'ов RecyclerView с данными адаптера.
 * Вызывается перед сохранением, чтобы собрать актуальный текст из EditText.
 */
fun RecyclerView.syncTextsFromRecyclerView(adapter: ContentBlockAdapter) {
    for (i in 0 until adapter.itemCount) {
        val holder = findViewHolderForAdapterPosition(i)
        if (holder == null) continue
        
        when (holder) {
            is TextBlockViewHolder -> {
                val text = holder.getContent()
                adapter.updateDataOnly(i, text)
            }
            is ImageBlockViewHolder -> {
                val caption = holder.getContent()
                adapter.updateDataOnly(i, caption)
            }
            is TableBlockViewHolder -> {
                // Для таблицы данные уже актуальны
            }
        }
    }
}