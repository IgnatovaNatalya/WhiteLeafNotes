package ru.whiteleaf.notes.common.utils

import ru.whiteleaf.notes.presentation.note_edit.NoteSearchHighlights
import ru.whiteleaf.notes.presentation.note_edit.SearchMatch
import ru.whiteleaf.notes.presentation.note_edit.SearchMatchTarget

/**
 * Подсвечивает все вхождения поисковой фразы [query] одновременно в заголовке и
 * в содержимом заметки, возвращая готовые спаны и объединённый список совпадений
 * в порядке «сверху вниз по экрану»: сначала заголовок, затем контент.
 *
 * Обeртка над [highlightAllMatches]
 *
 * @param title заголовок заметки
 * @param content содержимое заметки
 * @param query поисковая фраза
 * @param highlightColor цвет фона для подсветки совпадений
 */

fun highlightNoteMatches(
    title: String,
    content: String,
    query: String,
    highlightColor: Int,
    ignoreCase: Boolean = true
): NoteSearchHighlights {
    val (titleSp, titleRaw) = highlightAllMatches(title, query, highlightColor, ignoreCase)
    val (contentSp, contentRaw) = highlightAllMatches(content, query, highlightColor, ignoreCase)

    val matches = buildList {
        titleRaw.forEach { (s, e) -> add(SearchMatch(s, e, SearchMatchTarget.TITLE)) }
        contentRaw.forEach { (s, e) -> add(SearchMatch(s, e, SearchMatchTarget.CONTENT)) }
    }
    return NoteSearchHighlights(titleSp, contentSp, matches)
}