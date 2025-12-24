package ru.whiteleaf.notes.common.utils

import android.widget.EditText
import android.widget.ImageButton
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import ru.whiteleaf.notes.common.utils.TextWatcherManager.setupEditText
import java.util.regex.Pattern

object TextWatcherScrollManager {

    fun setupScrollDetection(
        editText: EditText,
        scrollView: NestedScrollView,
        button: ImageButton,
        onVisibilityChanged: ((Boolean) -> Unit)? = null
    ) {
        button.hide()

        val scrollCheck = {
            val canScrollDown = scrollView.canScrollVertically(1)
            if (canScrollDown) {
                button.show()
                onVisibilityChanged?.invoke(true)
            } else {
                button.hide()
                onVisibilityChanged?.invoke(false)
            }
        }

        setupEditText(editText) { editText.postDelayed({ scrollCheck() }, 50) }

        scrollView.viewTreeObserver.addOnScrollChangedListener { scrollCheck() }

        button.setOnClickListener { scrollToNextParagraph(editText, scrollView) }

        button.setOnLongClickListener {
            scrollView.smoothScrollTo(0, scrollView.getChildAt(0)?.height ?: 0)
            true
        }

    }
}

private fun scrollToNextParagraph(editText: EditText, scrollView: NestedScrollView) {
    val text = editText.text.toString()
    val currentPosition = editText.selectionStart.coerceAtLeast(0)

    val nextParagraphPattern = "\n\n\n"
    val pattern = Pattern.compile(nextParagraphPattern)
    val matcher = pattern.matcher(text)

    var nextParagraphPosition = -1

    while (matcher.find()) {
        if (matcher.start() > currentPosition) {
            nextParagraphPosition = matcher.start()
            break
        }
    }

    if (nextParagraphPosition != -1) {
        //nextParagraphPosition это где начинается паттерн нового абзаца  + 2 это чтобы чуть ниже сместиться
        val targetPosition = nextParagraphPosition + 2

        // 1. Устанавливаем курсор там где начинается сам текст
        val targetCursorPosition = nextParagraphPosition + nextParagraphPattern.length
        editText.setSelection(targetCursorPosition)
        editText.requestFocus()

        // 2. Даем время на обновление layout
        editText.post {
            // 3. Вычисляем позицию для прокрутки
            val line = editText.layout.getLineForOffset(targetPosition)
            val lineTop = editText.layout.getLineTop(line)
            val editTextTop = editText.top
            val targetScrollY = editTextTop + lineTop

            // 4. Прокручиваем так, чтобы курсор был у верха
            scrollView.smoothScrollTo(0, targetScrollY)

        }
    } else {
        scrollView.smoothScrollTo(0, scrollView.getChildAt(0)?.height ?: 0)
        editText.setSelection(editText.text?.length ?: 0)
        editText.requestFocus()
    }
}

private fun ImageButton.hide() {
    this.isVisible = false
}

private fun ImageButton.show() {
    this.isVisible = true
}