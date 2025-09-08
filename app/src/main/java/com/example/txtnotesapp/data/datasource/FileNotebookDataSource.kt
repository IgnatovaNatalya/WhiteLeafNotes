package com.example.txtnotesapp.data.datasource

import android.content.Context
import android.os.Build
import android.os.Environment
import android.util.Log
import java.io.File

class FileNotebookDataSource(private val context: Context) {

   val baseDir: File by lazy {
        File(context.filesDir, DEFAULT_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun getNotebookDir(name: String): File {
        return File(baseDir, name).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    /**
     * Получает все существующие записные книжки (папки)
     */
    fun getAllNotebooks(): List<File> {
        return try {
            baseDir.listFiles()?.filter { file ->
                file.isDirectory && !file.isHidden && file.name != ".trashed"
            } ?: emptyList()
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет доступа к директории: ${e.message}")
            emptyList()
        }
    }

    /**
     * Подсчитывает количество заметок в записной книжке
     */
    fun getNoteCount(notebookDir: File): Int {
        return try {
            notebookDir.listFiles()?.count { file ->
                file.isFile && file.name.endsWith(".txt") && !file.isHidden
            } ?: 0
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет доступа к подсчету файлов: ${e.message}")
            0
        }
    }

    /**
     * Получает общий размер всех заметок в записной книжке (в байтах)
     */
    fun getNotebookSize(notebookDir: File): Long {
        return try {
            notebookDir.listFiles()?.filter { file ->
                file.isFile && file.name.endsWith(".txt")
            }?.sumOf { it.length() } ?: 0L
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет доступа к размеру файлов: ${e.message}")
            0L
        }
    }

    /**
     * Удаляет записную книжку со всеми заметками внутри
     */
    fun deleteNotebook(notebookDir: File): Boolean {
        return try {
            if (notebookDir.exists() && notebookDir.isDirectory) {
                // Сначала удаляем все файлы в папке
                notebookDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
                // Затем удаляем саму папку
                notebookDir.delete()
            } else {
                false
            }
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет прав на удаление: ${e.message}")
            false
        }
    }

    /**
     * Переименовывает записную книжку
     */
    fun renameNotebook(oldDir: File, newName: String): Boolean {
        return try {
            // Проверяем, что новое имя не пустое и не содержит запрещенных символов
            if (newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
                return false
            }

            val newDir = File(newName)

            // Проверяем, что папка с таким именем не существует
            if (newDir.exists()) {
                return false
            }

            // Переименовываем папку
            oldDir.renameTo(newDir)
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет прав на переименование: ${e.message}")
            false
        }
    }

    /**
     * Проверяет, существует ли записная книжка с указанным именем
     */
    fun notebookExists(name: String): Boolean {
        return try {
            val dir = File(name)
            dir.exists() && dir.isDirectory
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет доступа к проверке существования: ${e.message}")
            false
        }
    }

    /**
     * Получает дату последнего изменения записной книжки
     * (дата изменения последней заметки в книжке)
     */
    fun getLastModifiedDate(notebookDir: File): Long {
        return try {
            notebookDir.listFiles()?.filter { file ->
                file.isFile && file.name.endsWith(".txt")
            }?.maxOfOrNull { it.lastModified() } ?: notebookDir.lastModified()
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет доступа к дате изменения: ${e.message}")
            notebookDir.lastModified()
        }
    }

    /**
     * Создает временную резервную копию записной книжки
     */
    fun createBackup(notebookDir: File): File? {
        return try {
            val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
            val backupFile =
                File(backupDir, "${notebookDir.name}_backup_${System.currentTimeMillis()}.zip")

            // Здесь можно реализовать архивацию файлов
            // Для простоты пока просто возвращаем файл для backup
            backupFile
        } catch (e: Exception) {
            Log.e("FileNotebookDataSource", "Ошибка создания бэкапа: ${e.message}")
            null
        }
    }

    /**
     * Проверяет доступность хранилища для записи
     */
    fun isStorageWritable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
        }
    }

    /**
     * Проверяет доступность хранилища для чтения
     */
    fun isStorageReadable(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            Environment.getExternalStorageState() in setOf(
                Environment.MEDIA_MOUNTED,
                Environment.MEDIA_MOUNTED_READ_ONLY
            )
        }
    }

    /**
     * Получает статистику по использованию хранилища
     */
    fun getStorageStats(): Map<String, Any> {
        return try {
            val notebooks = getAllNotebooks()
            val totalNotes = notebooks.sumOf { getNoteCount(it) }
            val totalSize = notebooks.sumOf { getNotebookSize(it) }

            mapOf(
                "totalNotebooks" to notebooks.size,
                "totalNotes" to totalNotes,
                "totalSizeBytes" to totalSize,
                "totalSizeMB" to totalSize / (1024 * 1024)
            )
        } catch (e: Exception) {
            Log.e("FileNotebookDataSource", "Ошибка получения статистики: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Очищает кэш временных файлов
     */
    fun clearCache(): Boolean {
        return try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { it.delete() }
            true
        } catch (e: Exception) {
            Log.e("FileNotebookDataSource", "Ошибка очистки кэша: ${e.message}")
            false
        }
    }
    companion object {
        const val DEFAULT_DIR = "txtNotes"
    }
}