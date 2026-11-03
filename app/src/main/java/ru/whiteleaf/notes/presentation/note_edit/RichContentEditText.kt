package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import android.text.SpannableString
import android.text.TextWatcher
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

/**
 * Rich EditText для отображения изображений и таблиц
 * Хранит plain text (markdown) и рендерит его с ImageSpan + TableReplacementSpan
 */
class RichContentEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var plainText: String = ""
    private var renderedText: SpannableString = SpannableString("")

    private var imageTapListener: ((filename: String) -> Unit)? = null
    private var tableCellEditListener: ((row: Int, col: Int) -> Unit)? = null

    // Флаг для предотвращения рекурсии при обновлении текста
    private var isUpdatingText = false

    init {
        setOnClickListener {
            // Обработка клика по изображению
            if (text is SpannableString) {
                val position = selectionStart
                val filename =
                    RichTextRenderer.getImageAtPosition(text as SpannableString, position)
                filename?.let { imageTapListener?.invoke(it) }
            }
        }
    }

    /**
     * Устанавливает рендеринговый контент (SpannableString с ImageSpan и TableReplacementSpan)
     */
    fun setRichContent(spannable: SpannableString) {
        isUpdatingText = true
        renderedText = spannable
        super.setText(spannable)
        isUpdatingText = false
    }

    /**
     * Получает plain text (markdown) для сохранения
     */
    fun getPlainText(): String = plainText

    /**
     * Устанавливает plain text и перестраивает рендеринг
     */
    fun setPlainText(content: String) {
        if (content == plainText) return

        plainText = content
        val spannable = RichTextRenderer.render(context, content, null, tableCellEditListener)
        isUpdatingText = true

        // Сохраняем позицию курсора
        val cursorPos = selectionStart

        super.setText(spannable)

        // Восстанавливаем позицию курсора
        if (cursorPos >= 0 && cursorPos <= spannable.length) {
            setSelection(cursorPos.coerceIn(0, spannable.length))
        }

        isUpdatingText = false
    }

    /**
     * Получает текущую позицию курсора в plain text
     */
    fun getCursorPosition(): Int {
        return selectionStart
    }

    /**
     * Устанавливает позицию курсора
     */
    fun setCursorPosition(position: Int) {
        if (position >= 0 && position <= text?.length ?: 0) {
            setSelection(position)
        }
    }

    /**
     * Вставляет текст в текущую позицию курсора
     */
    fun insertText(text: String) {
        val start = selectionStart
        val end = selectionEnd
        val currentText = plainText

        val newText = currentText.substring(0, start) + text + currentText.substring(end)
        setPlainText(newText)

        // Восстанавливаем позицию курсора
        val newCursorPos = start + text.length
        setCursorPosition(newCursorPos)
    }

    /**
     * Удаляет символы в текущей позиции курсора
     */
    fun deleteText(count: Int) {
        val cursorPos = selectionStart
        val currentText = plainText

        val newStart = (cursorPos - count).coerceAtLeast(0)
        val newText = currentText.substring(0, newStart) + currentText.substring(cursorPos)
        setPlainText(newText)
        setCursorPosition(newStart)
    }

    /**
     * Устанавливает слушатель кликов по изображениям
     */
    fun setImageTapListener(listener: (filename: String) -> Unit) {
        imageTapListener = listener
    }

    /**
     * Устанавливает слушатель кликов по ячейкам таблиц
     */
    fun setTableCellEditListener(listener: (row: Int, col: Int) -> Unit) {
        tableCellEditListener = listener
    }

    override fun addTextChangedListener(watcher: TextWatcher?) {
        if (isUpdatingText) return
        super.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdatingText) return
                // Обновляем plain text из видимого текста (убираем spans)
                plainText = s?.toString() ?: ""
            }

            override fun afterTextChanged(s: android.text.Editable?) {}
        })
    }

    /**
     * Обновляет plain text без перестройки рендера (для авто-сохранения)
     */
    fun updatePlainTextWithoutRender(newText: String) {
        plainText = newText
    }

    /**
     * Получает plain text для сохранения
     */
    fun getContentForSave(): String {
        // Берём plain text, игнорируя изменения от рендера
        return plainText
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection? {
        val base = super.onCreateInputConnection(outAttrs) ?: return null
        // mutable = false — не вмешиваемся в изменения текста
        return object : InputConnectionWrapper(base, false) {
            override fun sendKeyEvent(event: KeyEvent): Boolean {
                Log.e("IME_DEBUG", "sendKeyEvent: keyCode=${event.keyCode}, action=${event.action}")

                if (event.action == KeyEvent.ACTION_DOWN && event.keyCode == KeyEvent.KEYCODE_DEL) {
                    // Принудительно удаляем символ перед курсором
                    val editable = text
                    val cursor = selectionStart
                    if (editable != null && cursor > 0) {
                        editable.delete(cursor - 1, cursor)
                        Log.e("IME_DEBUG", "delete symbol(s)")
                        return true // Событие обработано, дальше не передаем
                    }
                }
                return super.sendKeyEvent(event)
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                Log.e(
                    "IME_DEBUG",
                    "deleteSurroundingText: before=$beforeLength, after=$afterLength"
                )
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                Log.e("IME_DEBUG", "commitText: text='$text'")
                return super.commitText(text, newCursorPosition)
            }

            override fun setComposingText(text: CharSequence?, newCursorPosition: Int): Boolean {
                Log.e("IME_DEBUG", "setComposingText: text='$text'")
                return super.setComposingText(text, newCursorPosition)
            }

        }
    }
}
