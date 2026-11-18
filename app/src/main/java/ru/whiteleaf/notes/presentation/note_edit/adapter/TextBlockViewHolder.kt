package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * ViewHolder для текстового блока.
 * Обновляет контент только при потере фокуса, чтобы не сбрасывать курсор при вводе.
 */
class TextBlockViewHolder(
    private val editText: EditText
) : BlockViewHolder(editText) {

    private var textWatcher: TextWatcher? = null
    private var onTextChanged: ((String) -> Unit)? = null
    private var currentText: String = ""

    override fun bind(block: Any, onTextChanged: ((String) -> Unit)?) {
        this.onTextChanged = onTextChanged
        
        // Удаляем предыдущий watcher
        textWatcher?.let { editText.removeTextChangedListener(it) }
        
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                currentText = s?.toString() ?: ""
            }
        }
        
        editText.addTextChangedListener(textWatcher)
        editText.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                // При потере фокуса отправляем обновлённый текст
                val text = editText.text?.toString() ?: ""
                if (currentText != text) {
                    currentText = text
                    onTextChanged?.invoke(text)
                }
            }
        }
        
        val text = (block as? ContentBlock.Text)?.text ?: ""
        editText.setText(text)
        currentText = text
        editText.visibility = View.VISIBLE
    }

    /**
     * Возвращает актуальный текст из EditText.
     * Сначала синхронизирует currentText с editText.text.
     */
    override fun getContent(): String {
        // Синхронизируем с EditText
        val text = editText.text?.toString() ?: ""
        if (currentText != text) {
            currentText = text
        }
        return currentText
    }

    override fun toString(): String {
        return "TextBlockViewHolder(text='${editText.text}')"
    }
}