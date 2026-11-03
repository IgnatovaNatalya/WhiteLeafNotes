package ru.whiteleaf.notes.common.utils

import java.util.UUID

object FileUtils {

    const val FILE_NAME_PREFIX = "note_"

    fun sanitizeFileName(input: String): String {
        val cleaned = input.replace(Regex("""[/\\:*?"<>|]"""), "")
        return cleaned
    }

    fun generateNoteId(): String {
        val timestamp = System.currentTimeMillis()
        return "$FILE_NAME_PREFIX${timestamp}"
    }

    fun generateImageId(): String = UUID.randomUUID().toString().replace("-", "").take(8)
}
