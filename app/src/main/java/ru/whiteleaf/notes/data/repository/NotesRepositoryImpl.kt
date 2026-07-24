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
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import java.io.File
import java.io.IOException

class NoteRepositoryImpl(
    private val context: Context,
    private val noteDataSource: FileNoteDataSource,
    private val encryptionRepository: EncryptionRepository,
) : NotesRepository {

    override suspend fun getNote(noteId: String, notebookPath: String?): Note {
        val file = noteDataSource.getNoteFile(notebookPath, noteId)
        //val isProtected =
        //    if (notebookPath != null) encryptionRepository.hasKey(notebookPath) else false
        return try {
            val lastModified = file.lastModified()
            val name = file.nameWithoutExtension
            val rawContent = noteDataSource.readNoteContent(file)
            val content = if (notebookPath != null && encryptionRepository.hasKey(notebookPath)) {
                encryptionRepository.decryptNote(notebookPath, rawContent)
            } else {
                rawContent
            }

            Note(
                id = name,
                title = if (name.startsWith(FILE_NAME_PREFIX)) "" else name,
                content = content,
                modifiedAt = lastModified,
                notebookPath = notebookPath
            )
        } catch (e: AuthenticationRequiredException) {
            // Ключ не разблокирован (истекло время действия биометрии или не было аутентификации)
            throw e
        } catch (e: Exception) {
            // Любая ошибка при чтении/расшифровке файла прерывает загрузку списка
            println("DEBUG: NoteRepositoryImpl get note: ${e.message}")
            throw IOException("Failed to read note ${file.name}", e)
        }
    }

    override suspend fun getNotes(notebookPath: String?): List<Note> {
        return withContext(Dispatchers.IO) {
            val dir =
                notebookPath?.let { File(noteDataSource.baseDir, it) } ?: noteDataSource.baseDir
            val isProtected = notebookPath?.let { encryptionRepository.hasKey(it) } ?: false

            // Если блокнот защищён и ключ не разблокирован – сразу бросаем исключение
            if (isProtected && !encryptionRepository.isUnlocked(notebookPath)) {
                throw AuthenticationRequiredException("Key is locked for getting notes in notebook $notebookPath")
            }

            noteDataSource.listFilesInDirectory(dir)
                ?.filter { it.isFile && it.name.endsWith(".txt") }
                ?.map { file ->
                    try {
                        val lastModified = file.lastModified()
                        val name = file.nameWithoutExtension
                        val rawContent = noteDataSource.readNoteContent(file)
                        val content = if (isProtected) {
                            encryptionRepository.decryptNote(notebookPath, rawContent)
                        } else {
                            rawContent
                        }

                        Note(
                            id = name,
                            title = if (name.startsWith(FILE_NAME_PREFIX)) "" else name,
                            content = content,
                            modifiedAt = lastModified,
                            notebookPath = notebookPath
                        )
                    } catch (e: AuthenticationRequiredException) {
                        // Ключ не разблокирован (истекло время действия биометрии или не было аутентификации)
                        throw e
                    } catch (e: Exception) {
                        // Любая ошибка при чтении/расшифровке файла прерывает загрузку списка
                        println("DEBUG: NoteRepositoryImpl get notes: ${e.message}")
                        throw IOException("Failed to read note ${file.name}", e)
                    }
                }?.sortedByDescending { it.modifiedAt } ?: emptyList()
        }
    }

    override suspend fun saveNote(note: Note) {
        withContext(Dispatchers.IO) {
            try {
                val isEncrypted =
                    if (note.notebookPath != null) encryptionRepository.hasKey(note.notebookPath) else true

                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)

                val content =
                    if (note.notebookPath != null && isEncrypted) encryptionRepository.encryptNote(
                        note.notebookPath,
                        note.content
                    )
                    else note.content

                noteDataSource.writeNoteContent(noteFile, content)

                //записываем в дату обновления файла дату существующую заметки, чтобы даты не слетали при сохранении
                if (note.modifiedAt > 0) noteDataSource.setNoteDate(
                    noteFile,
                    note.modifiedAt
                )
            } catch (e: AuthenticationRequiredException) {
                // Ключ не разблокирован (истекло время действия биометрии или не было аутентификации)
                throw e
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
                Log.e("NoteRepositoryImpl", "Ошибка удаления заметки: ${e.message}")
                throw IOException("Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    override suspend fun moveNote(note: Note, targetNotebookPath: String?) {
        withContext(Dispatchers.IO) {
            try {
                val sourceFile = noteDataSource.getNoteFile(note.notebookPath, note.id)
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

    override suspend fun renameNote(note: Note, newName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                if (newName == "") {
                    throw IOException("Недопустимое имя файла")
                }

                // Используем noteDataSource для проверки существования файла
                if (noteDataSource.existsNote(note.notebookPath ?: "", newName)) {
                    throw IOException("Файл с таким именем уже существует")
                }

                // Получаем файлы через noteDataSource
                val oldFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)
                val newFile = noteDataSource.getNoteFile(note.notebookPath ?: "", newName)

                // Используем noteDataSource для операции переименования/перемещения
                if (oldFile.exists()) {
                    noteDataSource.moveFile(oldFile, newFile)
                }
                newName
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

    override suspend fun existsNote(notebookPath: String?, noteId: String): Boolean {
        return try {
            noteDataSource.existsNote(notebookPath, noteId)
        } catch (_: Exception) {
            // Если произошла ошибка считаем что заметки нет
            false
        }
    }

    override suspend fun getRecentNoteTitle(
        notebookPath: String?,
        noteId: String
    ): String {
        val name = noteDataSource.getNoteFile(notebookPath, noteId).nameWithoutExtension
        return try {
            if (name.startsWith(FILE_NAME_PREFIX)) "Без названия" else name
        } catch (e: Exception) {
            println("DEBUG: NoteRepositoryImpl getRecentNoteTitle: ${e.message}")
            throw IOException("Failed to get  ${name}", e)
        }
    }


    override suspend fun updateNoteDate(note: Note, newTimestamp: Long) {
        withContext(Dispatchers.IO) {
            try {
                val noteFile = noteDataSource.getNoteFile(note.notebookPath ?: "", note.id)

                if (!noteFile.exists()) {
                    throw IOException("Файл заметки не найден: ${note.id}")
                }

                // Обновляем дату в файловой системе
                val success = noteDataSource.setNoteDate(noteFile, newTimestamp)

                if (!success) {
                    throw IOException("Не удалось обновить дату заметки")
                }
                Log.d(
                    "NoteRepository",
                    "Дата заметки ${note.id} изменена с ${note.modifiedAt} на $newTimestamp"
                )

            } catch (e: Exception) {
                Log.e("NoteRepository", "Ошибка обновления даты заметки: ${e.message}")
                throw IOException("Ошибка обновления даты заметки: ${e.message}")
            }
        }
    }

    // Дополнительно: удобный метод для перемещения в конкретный месяц
    override suspend fun moveNoteToMonth(note: Note, year: Int, month: Int) {
        withContext(Dispatchers.IO) {
            // Создаем дату первого числа месяца в 12:00:00
            val calendar = java.util.Calendar.getInstance().apply {
                set(year, month, 1, 12, 0, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }
            val newTimestamp = calendar.timeInMillis

            updateNoteDate(note, newTimestamp)
        }
    }

    override suspend fun encryptAllNotes(notebookPath: String) {
        withContext(Dispatchers.IO) {
            println("DEBUG: NoteRepositoryImpl: start encrypting notes in notebook $notebookPath")
            val dir = File(noteDataSource.baseDir, notebookPath)
            val files = noteDataSource.listFilesInDirectory(dir)
                ?.filter { it.isFile && it.name.endsWith(".txt") } ?: return@withContext

            for (file in files) {
                val plaintext = noteDataSource.readNoteContent(file)          // исходный текст
                val encrypted = encryptionRepository.encryptNote(notebookPath, plaintext) // шифруем

                val lastModified = file.lastModified()

                noteDataSource.writeNoteContent(file, encrypted)             // перезаписываем
                noteDataSource.setNoteDate(file, lastModified)
            }
            println("DEBUG: NoteRepositoryImpl: all notes encrypted in notebook $notebookPath")
        }
    }

    override suspend fun decryptAllNotes(notebookPath: String) {
        withContext(Dispatchers.IO) {
            val dir = File(noteDataSource.baseDir, notebookPath)
            val files = noteDataSource.listFilesInDirectory(dir)
                ?.filter { it.isFile && it.name.endsWith(".txt") } ?: return@withContext

            for (file in files) {
                val encrypted = noteDataSource.readNoteContent(file)
                val plaintext = encryptionRepository.decryptNote(notebookPath, encrypted)
                val lastModified = file.lastModified()
                noteDataSource.writeNoteContent(file, plaintext)
                noteDataSource.setNoteDate(file, lastModified)
            }
        }
    }
}