package com.example.txtnotesapp.common.utils

object FileUtils {

    const val FILE_NAME_PREFIX = "note_"

    fun sanitizeFileName(input: String, replacement: Char = '_'): String {
        //var cleaned = input.replace(Regex("[^a-zA-Z0-9_\\- .]"), "")
        var cleaned = input.replace(Regex("""[/\\:*?"<>|]"""), "")

        //cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        //    .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")

        //cleaned = cleaned.replace(Regex("[ ,]"), replacement.toString())
        //cleaned = cleaned.replace(Regex("$replacement+"), replacement.toString())
        //cleaned = cleaned.removePrefix(replacement.toString()).removeSuffix(replacement.toString())

        //return if (cleaned.isBlank()) "$FILE_NAME_PREFIX${System.currentTimeMillis()}" else cleaned
        return cleaned
    }

    fun generateNoteId(): String {
        val timestamp = System.currentTimeMillis()
        return "$FILE_NAME_PREFIX${timestamp}"
    }
}