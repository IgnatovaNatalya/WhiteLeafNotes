package ru.whiteleaf.notes.common.classes

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputConnectionWrapper
import androidx.appcompat.widget.AppCompatEditText

class BackSpaceFixEditText @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

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
                Log.e("IME_DEBUG", "deleteSurroundingText: before=$beforeLength, after=$afterLength")
                return super.deleteSurroundingText(beforeLength, afterLength)
            }

//            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
//                Log.e("IME_DEBUG","deleteSurroundingText: before=$beforeLength, after=$afterLength" )
//                if (beforeLength == 1 && afterLength == 0) {
//                    if (handleBackspace()) return true
//                } else
//                    if (selectionStart == selectionEnd) if (handleBackspace()) return true
//
//                return super.deleteSurroundingText(beforeLength, afterLength)
//            }
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