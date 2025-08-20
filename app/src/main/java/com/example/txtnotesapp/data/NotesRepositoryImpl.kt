package com.example.txtnotesapp.data

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.txtnotesapp.data.local.FileNoteDataSource
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

class NoteRepositoryImpl(
    private val context: Context,
    private val noteDataSource: FileNoteDataSource
) : NoteRepository {

    override suspend fun getNotes(notebookPath: String?): List<Note> {
        return withContext(Dispatchers.IO) {
            try {
                // Проверяем доступность хранилища для чтения
                if (!noteDataSource.isExternalStorageReadable()) {
                    throw IOException("Внешнее хранилище недоступно для чтения")
                }

                val notesDir = if (notebookPath != null) {
                    File(noteDataSource.baseDir, notebookPath)
                } else {
                    noteDataSource.baseDir
                }

                if (!notesDir.exists() || !notesDir.isDirectory) {
                    return@withContext emptyList()
                }

                notesDir.listFiles()?.filter { file ->
                    file.isFile && file.name.endsWith(".txt")
                }?.mapNotNull { file ->
                    try {
                        val content = file.readText()
                        Note(
                            title = file.nameWithoutExtension,
                            content = content,
                            createdAt = file.lastModified(),
                            notebookPath = notebookPath
                        )
                    } catch (e: Exception) {
                        null
                    }
                }?.sortedByDescending { it.createdAt } ?: emptyList()
            } catch (e: Exception) {
                // Логируем ошибку
                Log.e("NoteRepository", "Ошибка получения заметок: ${e.message}")
                emptyList()
            }
        }
    }

    override suspend fun saveNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                // Проверяем доступность хранилища для записи
                if (!noteDataSource.isExternalStorageWritable()) {
                    throw IOException("Внешнее хранилище недоступно для записи")
                }

                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.title)
                noteFile.writeText(note.content)

                // Обновляем время последнего изменения
                if (note.createdAt > 0) {
                    noteFile.setLastModified(note.createdAt)
                }
            } catch (e: Exception) {
                Log.e("NoteRepository", "Ошибка сохранения заметки: ${e.message}")
                throw IOException("Ошибка сохранения заметки: ${e.message}")
            }
        }
    }

    // Аналогично обновляем другие методы с проверкой доступности хранилища

    override suspend fun getNoteByTitle(noteTitle: String, notebookPath: String?): Note? {
        return withContext(Dispatchers.IO) {
            try {

                if (!noteDataSource.isExternalStorageReadable()) {
                    throw IOException("Внешнее хранилище недоступно для чтения")
                }

                val noteFile = noteDataSource.getNoteFile(notebookPath ?: "", noteTitle)
                if (!noteFile.exists()) {
                    return@withContext null
                }

                val content = noteFile.readText()
                Note(
                    title = noteTitle,
                    content = content,
                    createdAt = noteFile.lastModified(),
                    notebookPath = notebookPath
                )
            } catch (e: Exception) {
                Log.e("NoteRepository", "Ошибка получения заметки по имени: ${e.message}")
                throw IOException("Ошибка получения заметки по имени: ${e.message}")

            }
        }
    }

     suspend fun saveNote1(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.title)
                noteFile.writeText(note.content)

                // Обновляем время последнего изменения
                if (note.createdAt > 0) {
                    noteFile.setLastModified(note.createdAt)
                }
            } catch (e: Exception) {
                throw IOException("Ошибка сохранения заметки: ${e.message}")
            }
        }
    }

    override suspend fun deleteNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {

                if (!noteDataSource.isExternalStorageWritable()) {
                    throw IOException("Внешнее хранилище недоступно для записи")
                }

                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.title)
                if (noteFile.exists()) {
                    noteFile.delete()
                }
            } catch (e: Exception) {
                throw IOException("Ошибка удаления заметки: ${e.message}")
                Log.e("NoteRepository", "Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    override suspend fun moveNote(note: Note, targetNotebookPath: String?) {
        withContext(Dispatchers.IO) {
            try {
                if (!noteDataSource.isExternalStorageWritable()) {
                    throw IOException("Внешнее хранилище недоступно для записи")
                }
                val sourceFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.title)
                val targetDir = if (targetNotebookPath != null) {
                    File(noteDataSource.baseDir, targetNotebookPath).apply { mkdirs() }
                } else {
                    noteDataSource.baseDir
                }

                val targetFile = File(targetDir, "${note.title}.txt")

                if (sourceFile.exists()) {
                    if (targetFile.exists()) {
                        throw IOException("Файл с таким именем уже существует в целевой папке")
                    }

                    if (!sourceFile.renameTo(targetFile)) {
                        // Если renameTo не сработал, копируем и удаляем оригинал
                        sourceFile.copyTo(targetFile)
                        sourceFile.delete()
                    }
                }
            } catch (e: Exception) {
                throw IOException("Ошибка перемещения заметки: ${e.message}")
                Log.e("NoteRepository", "Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    override suspend fun renameNote(note: Note, newName: String) {
        withContext(Dispatchers.IO) {
            try {
                if (!noteDataSource.isExternalStorageWritable()) {
                    throw IOException("Внешнее хранилище недоступно для записи")
                }

                val oldFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.title)
                val newFile = noteDataSource.getNoteFile(note.notebookPath ?: "", newName)

                if (newFile.exists()) {
                    throw IOException("Файл с таким именем уже существует")
                }

                if (oldFile.exists()) {
                    if (!oldFile.renameTo(newFile)) {
                        // Если renameTo не сработал, копируем и удаляем оригинал
                        oldFile.copyTo(newFile)
                        oldFile.delete()
                    }
                }
            } catch (e: Exception) {
                throw IOException("Ошибка переименования заметки: ${e.message}")
                Log.e("NoteRepository", "Ошибка переименования заметки: ${e.message}")
            }
        }
    }

    override suspend fun shareNote(note: Note): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                if (!noteDataSource.isExternalStorageReadable()) {
                    throw IOException("Внешнее хранилище недоступно для чтения")
                }

                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.title)
                if (!noteFile.exists()) {
                    return@withContext null
                }

                // Создаем временный файл для sharing
                val cacheDir = context.cacheDir
                val shareFile = File(cacheDir, "${note.title}.txt")
                noteFile.copyTo(shareFile, overwrite = true)

                // Получаем URI через FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )
            } catch (e: Exception) {
                throw IOException("Ошибка шаринга заметки: ${e.message}")
                Log.e("NoteRepository", "Ошибка шаринга заметки: ${e.message}")
                null
            }
        }
    }
}