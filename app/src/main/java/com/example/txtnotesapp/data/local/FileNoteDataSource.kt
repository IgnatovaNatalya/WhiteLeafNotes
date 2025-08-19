package com.example.txtnotesapp.data.local

import android.content.Context
import java.io.File

class FileNoteDataSource(private val context: Context) {
    internal val baseDir: File by lazy {
        File(context.filesDir, "notes").apply { mkdirs() }
    }

    fun getNoteFile(path: String, name: String): File {
        val dir = if (path.isNotEmpty()) File(baseDir, path) else baseDir
        return File(dir, "$name.txt")
    }

    // todo добавить  методы для чтения/записи файлов

}