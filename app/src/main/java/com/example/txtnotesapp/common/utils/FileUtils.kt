package com.example.txtnotesapp.common.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

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
}