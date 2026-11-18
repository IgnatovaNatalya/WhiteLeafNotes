package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * ViewHolder для текстового блока.
 */
class TextBlockViewHolder(itemView: View) : BlockViewHolder(itemView) {

    private val editText: EditText = itemView.findViewById(R.id.item_text_block_edit_text)

    private var textWatcher: TextWatcher? = null
    private var onTextChanged: ((String) -> Unit)? = null

    override fun bind(block: Any, onTextChanged: ((String) -> Unit)?) {
        this.onTextChanged = onTextChanged
        
        // Удаляем предыдущий watcher, чтобы избежать дублирования
        textWatcher?.let { editText.removeTextChangedListener(it) }
        
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                onTextChanged?.invoke(s?.toString() ?: "")
            }
        }
        
        editText.addTextChangedListener(textWatcher)
        editText.setText((block as? ContentBlock.Text)?.text ?: "")
        editText.visibility = View.VISIBLE
    }

    override fun getContent(): String {
        return editText.text?.toString() ?: ""
    }

    fun clearWatcher() {
        textWatcher?.let { editText.removeTextChangedListener(it) }
        textWatcher = null
    }

    override fun toString(): String {
        return "TextBlockViewHolder(text='${editText.text}')"
    }
}