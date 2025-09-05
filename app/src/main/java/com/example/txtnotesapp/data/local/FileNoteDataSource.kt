package com.example.txtnotesapp.data.local

import android.content.Context
import android.os.Environment
import java.io.File

class FileNoteDataSource(
    private val context: Context
) {


    fun getNoteFile(baseDir: File, notebookPath: String, noteId: String): File {
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
    fun getAllNotebooks(baseDir: File): List<File> {
        return baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

}