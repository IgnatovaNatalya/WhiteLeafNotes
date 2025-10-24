package ru.whiteleaf.notes.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import ru.whiteleaf.notes.common.utils.FileUtils.FILE_NAME_PREFIX
import ru.whiteleaf.notes.data.datasource.FileNoteDataSource
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.whiteleaf.notes.data.config.NotebookConfigManager
import java.io.File
import java.io.IOException

class NoteRepositoryImpl(
    private val context: Context,
    private val noteDataSource: FileNoteDataSource,
    private val configManager: NotebookConfigManager
) : NotesRepository {

//    override suspend fun getNotes(notebookPath: String?): List<Note> {
//        return withContext(Dispatchers.IO) {
//            val dir =
//                notebookPath?.let { File(noteDataSource.baseDir, it) } ?: noteDataSource.baseDir
//
//            noteDataSource.listFilesInDirectory(dir)
//                ?.filter { it.isFile && it.name.endsWith(".txt") }
//                ?.mapNotNull { file ->
//                    try {
//                        val name = file.nameWithoutExtension
//                        val content = noteDataSource.readNoteContent(file)
//                        Note(
//                            id = name,
//                            title = if (name.startsWith(FILE_NAME_PREFIX)) "" else name,
//                            content = content,
//                            modifiedAt = file.lastModified(),
//                            notebookPath = notebookPath
//                        )
//                    } catch (_: Exception) {
//                        null
//                    }
//                }?.sortedByDescending { it.modifiedAt } ?: emptyList()
//
//        }
//    }

    override suspend fun getNotes(notebookPath: String?): List<Note> {
        return try {
            // Просто делегируем DataSource
            // Если нужна биометрия - получим SecurityException
            loadNotesFromDataSource(notebookPath)
        } catch (e: SecurityException) {
            // Сообщаем UI, что нужна биометрия
            throw BiometricRequiredException(notebookPath, e)
        } catch (e: Exception) {
            // Другие ошибки

            throw NotesLoadingException("Failed to load notes", e)
                //emptyList<Note>()
        }
    }

    private suspend fun loadNotesFromDataSource(notebookPath: String?): List<Note> {
        return withContext(Dispatchers.IO) {

            val directory = if (!notebookPath.isNullOrEmpty()) {
                File(noteDataSource.baseDir, notebookPath)
            } else {
                noteDataSource.baseDir
            }

            val files =  noteDataSource.listFilesInDirectory(directory) ?:return@withContext emptyList()

            files.filter { it.isFile && it.name.endsWith(".txt") }
                .mapNotNull { file ->
                    try {
                        val name = file.nameWithoutExtension
                        val content = noteDataSource.readNoteContent(file, notebookPath ?: "")
                        val lastModified = file.lastModified()

                        Note( // Явно создаем объект Note
                            id = name,
                            title = if (name.startsWith(FILE_NAME_PREFIX)) "" else name,
                            content = content,
                            notebookPath = notebookPath,
                            modifiedAt = lastModified
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.sortedByDescending { it.id }
        }
    }


    override suspend fun saveNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)
                noteDataSource.writeNoteContent(noteFile, note.content)

                if (note.modifiedAt > 0) noteDataSource.setFileLastModified(
                    noteFile,
                    note.modifiedAt
                )

            } catch (e: Exception) {
                Log.e("NoteRepository", "Ошибка сохранения заметки: ${e.message}")
                throw IOException("Ошибка сохранения заметки: ${e.message}")
            }
        }
    }

    override suspend fun deleteNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                noteDataSource.deleteNote(note.notebookPath ?: "", note.id)
            } catch (e: Exception) {
                Log.e("NoteRepository", "Ошибка удаления заметки: ${e.message}")
                throw IOException("Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    override suspend fun moveNote(note: Note, targetNotebookPath: String?) {
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)
                val targetDir = if (targetNotebookPath != null) {
                    File(noteDataSource.baseDir, targetNotebookPath).apply {
                        noteDataSource.createDirectory(this)
                    }
                } else {
                    noteDataSource.baseDir
                }

                val targetFile = File(targetDir, "${note.id}.txt")

                if (sourceFile.exists()) {
                    if (targetFile.exists()) {
                        throw IOException("Файл с таким именем уже существует в целевой папке")
                    }

                    noteDataSource.moveFile(sourceFile, targetFile)
                }
            } catch (e: Exception) {
                throw IOException("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    override suspend fun renameNote(note: Note, newId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (newId == "") {
                    throw IOException("Недопустимое имя файла")
                }

                // Используем noteDataSource для проверки существования файла
                if (noteDataSource.existsNote(note.notebookPath ?: "", newId)) {
                    throw IOException("Файл с таким именем уже существует")
                }

                // Получаем файлы через noteDataSource
                val oldFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)
                val newFile = noteDataSource.getNoteFile(note.notebookPath ?: "", newId)

                // Используем noteDataSource для операции переименования/перемещения
                if (oldFile.exists()) {
                    noteDataSource.moveFile(oldFile, newFile)
                }
                newId
            } catch (e: Exception) {
                Log.e("NoteRepository", "Ошибка переименования заметки: ${e.message}")
                throw IOException("Ошибка переименования заметки: ${e.message}")
            }
        }
    }

    override suspend fun shareNoteFile(note: Note): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)
                if (!noteFile.exists()) {
                    return@withContext null
                }

                // Создаем временный файл для sharing
                val cacheDir = context.cacheDir
                val shareFile = File(cacheDir, "${note.id}.txt")
                noteFile.copyTo(shareFile, overwrite = true)

                // Получаем URI через FileProvider
                FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    shareFile
                )
            } catch (_: Exception) {
                null
            }
        }
    }

    override suspend fun getAllNotes(notebooks: List<Notebook>): List<Note> {

        val allNotes = mutableListOf<Note>()

        allNotes.addAll(getNotes(null))
        notebooks.forEach { notebook ->
            allNotes.addAll(getNotes(notebook.path))
        }
        return allNotes
    }

    override suspend fun existsNote(notebookPath: String, noteId: String): Boolean {
        return try {
            noteDataSource.existsNote(notebookPath, noteId)
        } catch (_: Exception) {
            // Если произошла ошибка считаем что заметки нет
            false
        }
    }
}