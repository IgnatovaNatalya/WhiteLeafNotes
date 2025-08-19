package com.example.txtnotesapp.data

import com.example.txtnotesapp.data.local.FileNoteDataSource
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class NoteRepositoryImpl(
    private val dataSource: FileNoteDataSource
) : NoteRepository {

    override suspend fun getNotes(notebookPath: String?): List<Note> {
        return withContext(Dispatchers.IO) {
            val dir = notebookPath?.let { File(dataSource.baseDir, it) } ?: dataSource.baseDir
            dir.listFiles()?.filter { it.isFile && it.name.endsWith(".txt") }
                ?.mapNotNull { file ->
                    try {
                        val name = file.nameWithoutExtension
                        val content = file.readText()
                        Note(
                            id = name,
                            content = content,
                            createdAt = file.lastModified(),
                            notebookPath = notebookPath
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
        }
    }

    // todo другие методы
}