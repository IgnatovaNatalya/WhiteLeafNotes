package com.example.whiteleafnotes.data.repository

import android.util.Log
import com.example.whiteleafnotes.data.datasource.FileNotebookDataSource
import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.repository.NotebookRepository
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
                            //modifiedAt = notebookDataSource.getLastModifiedDate(dir)
                        )
                    }
                //.sortedByDescending { it.modifiedAt }
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка получения записных книжек: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun createNotebook(name: String): Notebook {
        return withContext(Dispatchers.IO) {
            try {
                if (name.isBlank() || name.contains("/") || name.contains("\\")) {
                    throw IllegalArgumentException("Некорректное имя записной книжки")
                }

                if (notebookDataSource.notebookExists(name)) {
                    throw IOException("Записная книжка с таким именем уже существует")
                }

                notebookDataSource.getNotebookDir(name)

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

    override suspend fun renameNotebook(notebook: Notebook, newName: String) {//: Notebook {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем валидность нового имени
                if (newName.isBlank() || newName.contains("/") || newName.contains("\\")) {
                    throw IllegalArgumentException("Некорректное новое имя")
                }

                val oldDir = notebookDataSource.getNotebookDir(notebook.path)
                if (!notebookDataSource.renameNotebook(oldDir, newName)) {
                    throw IOException("Не удалось переименовать записную книжку")
                }

                // Возвращаем обновленную записную книжку
                val newDir = notebookDataSource.getNotebookDir(newName)
                Notebook(
                    path = newName,
                    createdAt = notebook.createdAt,
                    noteCount = notebookDataSource.getNoteCount(newDir),
                    //modifiedAt = System.currentTimeMillis()
                )
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
                        noteCount = notebookDataSource.getNoteCount(notebookDir),
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

    override suspend fun notebookExist(path: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                notebookDataSource.notebookExists(path)
            } catch (e: Exception) {
                Log.e("NotebookRepository", "Ошибка проверки существования записной книжки: ${e.message}")
                false
            }
        }
    }

}