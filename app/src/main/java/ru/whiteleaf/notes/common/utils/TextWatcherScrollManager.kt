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
            val canScrollDown = scrollView.isVisible && scrollView.canScrollVertically(1)
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
    //val currentPosition = editText.selectionStart.coerceAtLeast(0)
    val layout = editText.layout ?: return
    val currentPosition = getTopVisibleOffset(editText, scrollView, layout)

    val pattern = Pattern.compile("\n\n\n")
    val matcher = pattern.matcher(text)

    var nextParagraphPosition = -1
    while (matcher.find()) {
        if (matcher.start() > currentPosition) {
            nextParagraphPosition = matcher.start()
            break
        }
    }

    if (nextParagraphPosition != -1) {
        val targetPosition = nextParagraphPosition + 2
        val targetCursorPosition = nextParagraphPosition + 3

        editText.setSelection(targetCursorPosition)
        editText.requestFocus()

        editText.post {
            val line = editText.layout.getLineForOffset(targetPosition)
            val lineTop = editText.layout.getLineTop(line)
            val editTextTop = editText.top
            val targetScrollY = editTextTop + lineTop
            scrollView.smoothScrollTo(0, targetScrollY)
        }
    } else {
        scrollView.smoothScrollTo(0, scrollView.getChildAt(0)?.height ?: 0)
        editText.setSelection(editText.text?.length ?: 0)
        editText.requestFocus()
    }
}

/**
 * Возвращает смещение (в символах) строки, которая сейчас находится
 * у верхней кромки видимой области NestedScrollView.
 */
private fun getTopVisibleOffset(
    editText: EditText,
    scrollView: NestedScrollView,
    layout: android.text.Layout
): Int {
    if (editText.height == 0 || layout.lineCount == 0) return 0

    val etLoc = IntArray(2)
    val svLoc = IntArray(2)
    editText.getLocationInWindow(etLoc)
    scrollView.getLocationInWindow(svLoc)

    // Верх EditText в координатах контента скролл-вью
    val editTextTopInContent = (etLoc[1] - svLoc[1]) + scrollView.scrollY
    // Насколько глубоко внутрь EditText попала верхняя кромка вьюпорта
    val yInEditText = scrollView.scrollY - editTextTopInContent

    return when {
        yInEditText <= 0 -> 0
        yInEditText >= editText.height -> layout.getLineStart(layout.lineCount - 1)
        else -> {
            val line = layout.getLineForVertical(yInEditText)
            layout.getLineStart(line)
        }
    }
}

private fun ImageButton.hide() {
    this.isVisible = false
}

private fun ImageButton.show() {
    this.isVisible = true
}