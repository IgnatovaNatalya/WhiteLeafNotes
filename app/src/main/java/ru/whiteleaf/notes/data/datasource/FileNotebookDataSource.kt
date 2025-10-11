package ru.whiteleaf.notes.data.datasource

import android.content.Context
import android.util.Log
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
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

    fun deleteNotebook(notebookDir: File): Boolean {
        return try {
            if (notebookDir.exists() && notebookDir.isDirectory) {
                notebookDir.deleteRecursively()
            } else {
                false
            }
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет прав на удаление: ${e.message}")
            false
        }
    }

    fun renameNotebook(oldDir: File, newName: String): Boolean {
        return try {
            // Проверяем, что новое имя не пустое и не содержит запрещенных символов
            if (newName.isBlank() || newName.contains("/") || newName.contains("\\"))
                return false

            val newDir = File(oldDir.parentFile, newName)

            if (newDir.exists()) return false

            oldDir.renameTo(newDir)

        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет прав на переименование: ${e.message}")
            false
        }
    }

    fun notebookExists(name: String): Boolean {
        return try {
            val dir = File(baseDir, name)
            dir.exists() && dir.isDirectory
        } catch (e: SecurityException) {
            Log.e("FileNotebookDataSource", "Нет доступа к проверке существования: ${e.message}")
            false
        }
    }
}