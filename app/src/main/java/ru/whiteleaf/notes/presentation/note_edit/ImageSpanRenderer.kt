package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.text.SpannableString
import android.text.style.ImageSpan
import ru.whiteleaf.notes.common.AppConstants
import ru.whiteleaf.notes.common.utils.ImageStorageUtils

/**
 * Рендер изображений: конвертирует маркеры [IMG:filename] в ImageSpan
 */
object ImageSpanRenderer {

    private const val PLACEHOLDER_TEXT = "[IMG]"

    /**
     * Рендерит контент, заменяя маркеры изображений на ImageSpan
     */
    fun renderImages(context: Context, content: String, notebookPath: String?): SpannableString {
        val spannable = SpannableString(content)
        val pattern = Regex(AppConstants.IMAGE_MARKER_PATTERN)

        pattern.findAll(content).forEach { matchResult ->
            val filename = matchResult.groupValues.getOrNull(1) ?: return@forEach
            val start = matchResult.range.first
            val end = matchResult.range.last + 1

            val imageFile = ImageStorageUtils.getImageFile(context, extractNoteId(filename), filename, notebookPath)
            val drawable = if (imageFile.exists()) {
                Drawable.createFromPath(imageFile.absolutePath)?.apply {
                    setBounds(0, 0, intrinsicWidth, intrinsicHeight)
                }
            } else null

            drawable?.let {
                val imageSpan = ImageSpan(it, ImageSpan.ALIGN_BASELINE)
                spannable.setSpan(imageSpan, start, end, 0)
            }
        }

        return spannable
    }

    /**
     * Получает filename изображения по позиции в SpannableString
     */
    fun getImageMarkerAtPosition(spannable: SpannableString, position: Int): String? {
        val spans = spannable.getSpans(0, spannable.length, ImageSpan::class.java)
        spans.forEach { span ->
            val spanStart = spannable.getSpanStart(span)
            val spanEnd = spannable.getSpanEnd(span)
            if (position in spanStart until spanEnd) {
                val markerText = spannable.substring(spanStart, spanEnd)
                return extractFilenameFromMarker(markerText)
            }
        }
        return null
    }

    /**
     * Получает диапазон маркера изображения
     */
    fun getImageMarkerRange(spannable: SpannableString, position: Int): Pair<Int, Int>? {
        val spans = spannable.getSpans(0, spannable.length, ImageSpan::class.java)
        spans.forEach { span ->
            val spanStart = spannable.getSpanStart(span)
            val spanEnd = spannable.getSpanEnd(span)
            if (position in spanStart until spanEnd) {
                return Pair(spanStart, spanEnd)
            }
        }
        return null
    }

    /**
     * Извлекает noteId из filename (noteId_uuid.ext → noteId)
     */
    private fun extractNoteId(filename: String): String {
        val nameWithoutExt = filename.substringBeforeLast('.')
        val underscoreIndex = nameWithoutExt.indexOf('_')
        return if (underscoreIndex > 0) {
            nameWithoutExt.substring(0, underscoreIndex)
        } else {
            nameWithoutExt
        }
    }

    /**
     * Извлекает filename из маркера [IMG:filename]
     */
    private fun extractFilenameFromMarker(marker: String): String {
        return marker.removePrefix("[IMG:").removeSuffix("]")
    }
}
