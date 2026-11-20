package ru.whiteleaf.notes.presentation.note_edit.adapter

import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.BackgroundColorSpan
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import ru.whiteleaf.notes.domain.model.ContentBlock

/**
 * ViewHolder для текстового блока.
 * Обновляет контент через debounce (500мс) после окончания ввода.
 */
class TextBlockViewHolder(
    private val editText: EditText
) : BlockViewHolder(editText) {

    private var textWatcher: TextWatcher? = null
    private var onTextChanged: ((Int, String) -> Unit)? = null
    private var currentText: String = ""
    private var adapterPosition: Int = RecyclerView.NO_POSITION

    override fun bind(block: Any, onTextChanged: ((Int, String) -> Unit)?) {
        this.onTextChanged = onTextChanged
        
        // Удаляем предыдущий watcher
        textWatcher?.let { editText.removeTextChangedListener(it) }
        
        textWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val newText = s?.toString() ?: ""
                if (newText != currentText) {
                    currentText = newText
                    // Вызываем callback — debounce происходит в ContentBlockAdapter
                    onTextChanged?.invoke(adapterPosition, newText)
                }
            }
        }
        
        editText.addTextChangedListener(textWatcher)
        
        val text = (block as? ContentBlock.Text)?.text ?: ""
        editText.setText(text)
        currentText = text
        editText.visibility = View.VISIBLE
    }

    /**
     * Устанавливает позицию блока в адаптере.
     */
    fun setAdapterPosition(position: Int) {
        this.adapterPosition = position
        editText.tag = position
    }

    /**
     * Возвращает актуальный текст из EditText.
     */
    override fun getContent(): String {
        val text = editText.text?.toString() ?: ""
        if (currentText != text) {
            currentText = text
        }
        return currentText
    }

    override fun getEditText(): EditText? = editText

    /**
     * Подсвечивает диапазон текста в EditText.
     */
    fun highlightText(start: Int, end: Int, color: Int) {
        val text = editText.text ?: return
        val spannable = SpannableString(text.toString())
        
        // Удаляем старую подсветку
        spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
            .forEach { spannable.removeSpan(it) }
        
        // Добавляем новую подсветку
        if (start >= 0 && end <= spannable.length && start < end) {
            spannable.setSpan(
                BackgroundColorSpan(color),
                start,
                end,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            editText.editableText.replace(0, editText.text.length, spannable)
        }
    }

    /**
     * Очищает всю подсветку в EditText.
     */
    fun clearHighlight() {
        val text = editText.text ?: return
        val spannable = SpannableString(text.toString())
        
        spannable.getSpans(0, spannable.length, BackgroundColorSpan::class.java)
            .forEach { spannable.removeSpan(it) }
        
        editText.editableText.replace(0, editText.text.length, spannable)
    }

    override fun toString(): String {
        return "TextBlockViewHolder(position=$adapterPosition, text='${editText.text}')"
    }
}
