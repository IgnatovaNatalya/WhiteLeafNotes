package com.example.txtnotesapp.data.repository

import android.util.Log
import com.example.txtnotesapp.data.datasource.FileNotebookDataSource
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.NotebookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class NotebookRepositoryImpl(
    private val notebookDataSource: FileNotebookDataSource
) : NotebookRepository {


    override suspend fun getNotebooks(): List<Notebook> {
        return withContext(Dispatchers.IO) {
            try {
                notebookDataSource.getAllNotebooks()
                    .map { dir ->
                        Notebook(
                            path = dir.name,
                            createdAt = dir.lastModified(),
                            noteCount = notebookDataSource.getNoteCount(dir)
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
            try {
                // Проверяем валидность имени
                if (name.isBlank() || name.contains("/") || name.contains("\\")) {
                    throw IllegalArgumentException("Некорректное имя записной книжки")
                }

                // Проверяем, не существует ли уже папка с таким именем
                if (notebookDataSource.notebookExists(name)) {
                    throw IOException("Записная книжка с таким именем уже существует")
                }

                // Создаем папку
                val notebookDir = notebookDataSource.getNotebookDir(name)

                Notebook(
                    path = name,
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
            try {
                val notebookDir = notebookDataSource.getNotebookDir(notebook.path)
                if (!notebookDataSource.deleteNotebook(notebookDir)) {
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
            try {
                // Проверяем валидность нового имени
                if (newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
                    throw IllegalArgumentException("Некорректное новое имя")
                }

                val oldDir = notebookDataSource.getNotebookDir(notebook.path)
                if (!notebookDataSource.renameNotebook(oldDir, newName)) {
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
            try {
                val notebookDir = notebookDataSource.getNotebookDir(path)
                if (notebookDir.exists() && notebookDir.isDirectory) {
                    Notebook(
                        path = path,
                        createdAt = notebookDir.lastModified(),
                        noteCount = notebookDataSource.getNoteCount(notebookDir)
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

    //override suspend fun getNotebookStats(): Map<String, Any> {
    suspend fun getNotebookStats(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            try {
                val stats = notebookDataSource.getStorageStats()
                mapOf(
                    "totalNotebooks" to (stats["totalNotebooks"] as? Int ?: 0),
                    "totalNotes" to (stats["totalNotes"] as? Int ?: 0),
                    "totalSizeBytes" to (stats["totalSizeBytes"] as? Long ?: 0L),
                    "totalSizeMB" to (stats["totalSizeMB"] as? Long ?: 0L)
                )
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка получения статистики хранилища: ${e.message}")
                emptyMap()
            }
        }
    }
}