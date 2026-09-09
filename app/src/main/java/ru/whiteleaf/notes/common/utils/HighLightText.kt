package ru.whiteleaf.notes.common.utils

import android.text.Spannable
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
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


/**
 * Подсвечивает все вхождения подстроки [query] в тексте [text] заданным цветом.
 *
 * @param text исходный текст
 * @param query искомая подстрока
 * @param highlightColor цвет подсветки (например, Color.YELLOW или ContextCompat.getColor(...))
 * @param ignoreCase если true, поиск регистронезависимый
 * @return пару (SpannableString с наложенными спанами, список пар (start, end) для каждого совпадения)
 */

fun highlightAllMatches(
    text: String,
    query: String,
    highlightColor: Int,
    ignoreCase: Boolean = true
): Pair<SpannableString, List<Pair<Int, Int>>> {
    val spannable = SpannableString(text)
    val matches = mutableListOf<Pair<Int, Int>>()
    if (query.isEmpty()) return Pair(spannable, matches)

    // Выбираем способ сравнения
    val compareText = if (ignoreCase) text.lowercase() else text
    val compareQuery = if (ignoreCase) query.lowercase() else query

    var start = compareText.indexOf(compareQuery)
    while (start >= 0) {
        val end =
            start + query.length // длина берётся из оригинального запроса (не из lowerCase, чтобы сохранить длину)
        matches.add(start to end)
        spannable.setSpan(
            BackgroundColorSpan(highlightColor),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        start = compareText.indexOf(compareQuery, start + 1)
    }
    return Pair(spannable, matches)
}