package com.example.txtnotesapp.data.local

import android.content.Context
import android.os.Environment
import java.io.File

class FileNoteDataSource(private val context: Context) {

    val baseDir: File by lazy {
        File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), DEFAULT_DIR).apply {
            mkdirs()
        }
    }

    fun getNoteFile(notebookPath: String, noteId: String): File {
        val dir = if (notebookPath.isNotEmpty()) {
            File(baseDir, notebookPath).apply {
                if (!exists()) {
                    mkdirs()
                }
            }
        } else {
            baseDir
        }
        return File(dir, "$noteId.txt")
    }

    // Проверка разрешений на запись
    fun isExternalStorageWritable(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }

    // Проверка разрешений на чтение
    fun isExternalStorageReadable(): Boolean {
        return Environment.getExternalStorageState() in setOf(
            Environment.MEDIA_MOUNTED,
            Environment.MEDIA_MOUNTED_READ_ONLY
        )
    }

    // Получение всех папок-записных книжек
    fun getAllNotebooks(): List<File> {
        return baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    companion object {
        const val DEFAULT_DIR = "TxtNotesApp" //todo сделать хранение в настройках
    }
}