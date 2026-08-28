package ru.whiteleaf.notes.common.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.widget.TextView
import androidx.annotation.ColorInt

/**
 * Подсвечивает все вхождения подстроки [query] в тексте [text] цветом [highlightColor].
 * @param textView TextView, в который будет установлен текст с подсветкой.
 * @param text Исходный текст (может содержать переносы и пр.).
 * @param query Искомая подстрока (регистр учитывается, если не указано иное).
 * @param highlightColor Цвет подсветки (можно передать colorRes или ColorInt).
 * @param ignoreCase Флаг регистронезависимости (по умолчанию false).
 */
fun highlightMatches(
    textView: TextView,
    text: String,
    query: String,
    @ColorInt highlightColor: Int,
    ignoreCase: Boolean = false
) {
    if (query.isEmpty()) {
        textView.text = text
        return
    }

    val spannable = SpannableString(text)
    val compareText = if (ignoreCase) text.lowercase() else text
    val compareQuery = if (ignoreCase) query.lowercase() else query

    var startIndex = 0
    while (true) {
        val index = compareText.indexOf(compareQuery, startIndex)
        if (index == -1) break
        // Применяем цветовой span
        spannable.setSpan(
            ForegroundColorSpan(highlightColor),
            index,
            index + query.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        // Перемещаемся дальше (неперекрывающиеся вхождения)
        startIndex = index + query.length
    }

    textView.text = spannable
}