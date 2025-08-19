package com.example.txtnotesapp.data.local

import android.content.Context
import java.io.File

class FileNoteDataSource(private val context: Context) {
    val baseDir: File by lazy {
        File(context.filesDir, "notes").apply {
            if (!exists()) {
                mkdirs()
            }
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

    fun getAllNotebooks(): List<File> {
        return baseDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
    }

    fun createNotebook(name: String): File {
        val notebookDir = File(baseDir, name)
        if (!notebookDir.exists()) {
            notebookDir.mkdirs()
        }
        return notebookDir
    }
}