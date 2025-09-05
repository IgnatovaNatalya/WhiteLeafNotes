package com.example.txtnotesapp.data

import android.content.Context
import android.util.Log
import com.example.txtnotesapp.data.local.FileNoteDataSource
import com.example.txtnotesapp.data.local.FileNotebookDataSource
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.NotebookRepository
import com.example.txtnotesapp.domain.use_case.GetNotesDirectoryUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class NotebookRepositoryImpl(
    private val context: Context,
    private val notebookDataSource: FileNotebookDataSource,
    private val noteDataSource: FileNoteDataSource,
    private val getNotesDirectoryUseCase: GetNotesDirectoryUseCase
) : NotebookRepository {

    private suspend fun getNotesDirectory(): File {
        val customPath = getNotesDirectoryUseCase()

        return if (!customPath.isNullOrEmpty()) {
            File(customPath)
        } else {
            // Папка по умолчанию
            File(context.getExternalFilesDir(null), "txtNotes")
        }.apply {
            if (!exists()) mkdirs()
        }
    }

    override suspend fun getNotebooks(): List<Notebook> {
        return withContext(Dispatchers.IO) {
            val baseDir = getNotesDirectory()

            try {
                notebookDataSource.getAllNotebooks(baseDir)
                    .map { dir ->
                        Notebook(
                            path = dir.name,
                            //name = dir.name,
                            createdAt = dir.lastModified(),
                            noteCount = notebookDataSource.getNoteCount(baseDir,dir)
                        )
                    }
                    .sortedByDescending { it.createdAt }
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка получения записных книжек: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun createNotebook(name: String): Notebook {

        return withContext(Dispatchers.IO) {
            val baseDir = getNotesDirectory()
            try {
                // Проверяем валидность имени
                if (name.isBlank() || name.contains("/") || name.contains("\\")) {
                    throw IllegalArgumentException("Некорректное имя записной книжки")
                }

                // Проверяем, не существует ли уже папка с таким именем
                val existingNotebooks = notebookDataSource.getAllNotebooks(baseDir)
                if (existingNotebooks.any { it.name == name }) {
                    throw IOException("Записная книжка с таким именем уже существует")
                }

                // Создаем папку
                val notebookDir = notebookDataSource.getNotebookDir(baseDir, name)

                Notebook(
                    path = name,
                    //name = name,
                    createdAt = System.currentTimeMillis(),
                    noteCount = 0
                )
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка создания записной книжки: ${e.message}")
                throw IOException("Не удалось создать записную книжку: ${e.message}")
            }
        }
    }

    override suspend fun deleteNotebook(notebook: Notebook) {
        withContext(Dispatchers.IO) {
            val baseDir = getNotesDirectory()

            try {
                val notebookDir = notebookDataSource.getNotebookDir(baseDir,notebook.name)
                if (!notebookDataSource.deleteNotebook(baseDir,notebookDir)) {
                    throw IOException("Не удалось удалить записную книжку")
                }
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка удаления записной книжки: ${e.message}")
                throw IOException("Не удалось удалить записную книжку: ${e.message}")
            }
        }
    }

    override suspend fun renameNotebook(notebook: Notebook, newName: String) {
        withContext(Dispatchers.IO) {
            val baseDir = getNotesDirectory()
            try {
                // Проверяем валидность нового имени
                if (newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
                    throw IllegalArgumentException("Некорректное новое имя")
                }

                val oldDir = notebookDataSource.getNotebookDir(baseDir,notebook.name)
                if (!notebookDataSource.renameNotebook(baseDir,oldDir, newName)) {
                    throw IOException("Не удалось переименовать записную книжку")
                }
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка переименования записной книжки: ${e.message}")
                throw IOException("Не удалось переименовать записную книжку: ${e.message}")
            }
        }
    }

    override suspend fun getNotebookByPath(path: String): Notebook? {
        return withContext(Dispatchers.IO) {
            val baseDir = getNotesDirectory()

            try {
                val notebookDir = notebookDataSource.getNotebookDir(baseDir, path)
                if (notebookDir.exists() && notebookDir.isDirectory) {
                    Notebook(
                        path = path,
                        //name = path,
                        createdAt = notebookDir.lastModified(),
                        noteCount = notebookDataSource.getNoteCount(baseDir, notebookDir)
                    )
                } else {
                    null
                }
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка получения записной книжки: ${e.message}")
                null
            }
        }
    }

    // Дополнительный метод для получения статистики
    suspend fun getNotebooksWithStats(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val notebooks = getNotebooks()
                val totalNotes = notebooks.sumOf { it.noteCount }

                mapOf(
                    "totalNotebooks" to notebooks.size,
                    "totalNotes" to totalNotes,
                    "notebooks" to notebooks
                )
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка получения статистики: ${e.message}")
                emptyMap()
            }
        }
    }
}