package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import android.text.SpannableString
import ru.whiteleaf.notes.common.utils.TableParser

/**
 * Единый рендер: объединяет ImageSpan и TableReplacementSpan
 */
object RichTextRenderer {

    /**
     * Рендерит контент заметки с поддержкой изображений и таблиц
     */
    fun render(
        context: Context,
        content: String,
        notebookPath: String?,
        onCellClick: ((row: Int, col: Int) -> Unit)? = null
    ): SpannableString {
        if (content.isEmpty()) return SpannableString("")

        // Сначала рендерим таблицы
        var result = TableSpanRenderer.renderTables(context, content, onCellClick)

        // Затем рендерим изображения поверх таблиц
        result = ImageSpanRenderer.renderImages(context, result.toString(), notebookPath)

        return result
    }

    /**
     * Получает plain text из SpannableString (убирает spans)
     */
    fun getPlainText(spannable: SpannableString): String {
        return spannable.toString()
    }

    /**
     * Получает filename изображения по позиции курсора
     */
    fun getImageAtPosition(spannable: SpannableString, position: Int): String? {
        return ImageSpanRenderer.getImageMarkerAtPosition(spannable, position)
    }
}
