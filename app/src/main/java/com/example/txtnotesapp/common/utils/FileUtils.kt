package com.example.txtnotesapp.common.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.DocumentsContract
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {


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

    /**
     * Проверяет валидность имени файла/папки
     */
    fun isValidFileName(name: String): Boolean {
        if (name.isBlank() || name.length > 50) return false

        // Запрещенные символы в именах файлов
        val invalidChars = charArrayOf('/', '\\', ':', '*', '?', '"', '<', '>', '|')
        return name.none { invalidChars.contains(it) }
    }

    /**
     * Форматирует размер файла в читаемый вид
     */
    fun formatFileSize(size: Long): String {
        return when {
            size < 1024 -> "$size B"
            size < 1024 * 1024 -> "${size / 1024} KB"
            size < 1024 * 1024 * 1024 -> "${size / (1024 * 1024)} MB"
            else -> "${size / (1024 * 1024 * 1024)} GB"
        }
    }

    /**
     * Форматирует дату в читаемый вид
     */
    fun formatDate(timestamp: Long): String {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        return format.format(date)
    }

    /**
     * Создает уникальное имя для файла
     */
    fun generateUniqueFileName(baseName: String, extension: String = ".txt"): String {
        val timestamp = System.currentTimeMillis()
        return "${baseName}_${timestamp}$extension"
    }

    // Утилита для получения реального пути из URI
    fun getPathFromUri(context: Context, uri: Uri): String? {
        if (DocumentsContract.isDocumentUri(context, uri)) {
            if (isExternalStorageDocument(uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                val split = docId.split(":").toTypedArray()
                val type = split[0]
                if ("primary".equals(type, ignoreCase = true)) {
                    return Environment.getExternalStorageDirectory().toString() + "/" + split[1]
                }
            }
        }
        return null
    }

    private fun isExternalStorageDocument(uri: Uri): Boolean {
        return "com.android.externalstorage.documents" == uri.authority
    }
}