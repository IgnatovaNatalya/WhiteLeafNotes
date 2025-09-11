package com.example.txtnotesapp.data.datasource

import android.content.Context
import com.example.txtnotesapp.common.AppConstants.DEFAULT_DIR
import java.io.File


class FileNoteDataSource(
    private val context: Context
) {

    // Базовая директория во внутренней памяти
    val baseDir: File by lazy {
        File(context.filesDir, DEFAULT_DIR).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    fun getNoteFile(notebookPath: String, noteTitle: String): File {
        val dir = if (notebookPath.isNotEmpty()) {
            File(baseDir, notebookPath).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        } else {
            baseDir
        }
        return File(dir, "$noteTitle.txt")
    }

    // Получение всех папок-записных книжек
//    fun getAllNotebooks(): List<File> {
//        return baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
//    }

    // Получение всех заметок в конкретной записной книжке
//    fun getNotesInNotebook(notebookPath: String): List<File> {
//        val notebookDir = if (notebookPath.isNotEmpty()) {
//            File(baseDir, notebookPath)
//        } else {
//            baseDir
//        }
//
//        return if (notebookDir.exists() && notebookDir.isDirectory) {
//            notebookDir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") } ?: emptyList()
//        } else {
//            emptyList()
//        }
//    }

    // Удаление заметки
    fun deleteNote(notebookPath: String, noteId: String): Boolean {
        val file = getNoteFile(notebookPath, noteId)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    // Удаление записной книжки (со всеми заметками внутри)
    fun deleteNotebook(notebookPath: String): Boolean {
        if (notebookPath.isEmpty()) {
            return false // Нельзя удалить корневую директорию
        }

        val notebookDir = File(baseDir, notebookPath)
        return if (notebookDir.exists() && notebookDir.isDirectory) {
            notebookDir.deleteRecursively()
        } else {
            false
        }
    }

    // Проверка существования файла
    fun noteExists(notebookPath: String, noteId: String): Boolean {
        return getNoteFile(notebookPath, noteId).exists()
    }
}