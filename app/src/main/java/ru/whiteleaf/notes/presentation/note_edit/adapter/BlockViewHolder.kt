package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView

/**
 * Базовый ViewHolder для блоков контента.
 */
abstract class BlockViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    abstract fun bind(block: Any, onTextChanged: ((Int, String) -> Unit)?)
    abstract fun getContent(): String
    
    /**
     * Возвращает EditText для этого блока (если есть).
     */
    open fun getEditText(): EditText? = null
}