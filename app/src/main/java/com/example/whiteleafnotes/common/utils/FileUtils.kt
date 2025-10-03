package com.example.whiteleafnotes.common.utils

object FileUtils {

    const val FILE_NAME_PREFIX = "note_"

    fun sanitizeFileName(input: String): String {
        var cleaned = input.replace(Regex("""[/\\:*?"<>|]"""), "")
        return cleaned
    }

    fun generateNoteId(): String {
        val timestamp = System.currentTimeMillis()
        return "$FILE_NAME_PREFIX${timestamp}"
    }
}