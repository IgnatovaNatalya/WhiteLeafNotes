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
import ru.whiteleaf.notes.common.utils.FileUtils.generateNoteId
import ru.whiteleaf.notes.common.utils.FileUtils.sanitizeFileName
import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import java.io.File
import java.io.IOException
import kotlin.math.max
import kotlin.math.min

class NoteRepositoryImpl(
    private val context: Context,
    private val noteDataSource: FileNoteDataSource,
    private val encryptionRepository: EncryptionRepository,
) : NotesRepository {

    override suspend fun getNote(noteId: String, notebookPath: String?): Note {
        val file = noteDataSource.getNoteFile(notebookPath, noteId)
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

    override suspend fun getNotesList(notebookPath: String?): List<Note> {
        //получаем заметки для списка только с названиями, а контент берем только если названия нет
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
                        val contentPreview = name
                            .takeIf { it.startsWith(FILE_NAME_PREFIX) }
                            ?.let { noteDataSource.readNoteContent(file) }
                            ?.let {
                                if (isProtected) encryptionRepository.decryptNote(
                                    notebookPath,
                                    it
                                ) else it
                            }
                            ?.take(40) //часть контента для отображения вместо названия если нет названия
                            ?: ""

                        Note(
                            id = name,
                            title = if (name.startsWith(FILE_NAME_PREFIX)) "" else name,
                            content = contentPreview,
                            modifiedAt = lastModified,
                            notebookPath = notebookPath
                        )
                    } catch (e: AuthenticationRequiredException) {
                        // Ключ не разблокирован (истекло время действия биометрии или не было аутентификации)
                        throw e
                    } catch (e: Exception) {
                        // Любая ошибка при чтении/расшифровке файла прерывает загрузку списка
                        println("DEBUG: NoteRepositoryImpl get notes error: ${e.message}")
                        throw IOException("Failed to read note ${file.name}", e)
                    }
                }?.sortedByDescending { it.modifiedAt } ?: emptyList()
        }
    }

    override suspend fun getNotes(notebookPath: String?): List<Note> {
        //получаем заметки полностью
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
                        println("DEBUG: NoteRepositoryImpl get notes error: ${e.message}")
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
                val sourcePath = note.notebookPath

                // Если перемещение в ту же папку — ничего не делаем
                if (sourcePath == targetNotebookPath) {
                    return@withContext
                }

                val sourceProtected = sourcePath != null && encryptionRepository.hasKey(sourcePath)
                val targetProtected = targetNotebookPath != null && encryptionRepository.hasKey(
                    targetNotebookPath
                )

                // Проверяем, что ключи (если нужны) разблокированы
                if (sourceProtected && !encryptionRepository.isUnlocked(sourcePath)) {
                    throw AuthenticationRequiredException("Key is locked for source notebook $sourcePath")
                }
                if (targetProtected && !encryptionRepository.isUnlocked(targetNotebookPath)) {
                    throw AuthenticationRequiredException("Key is locked for target notebook $targetNotebookPath")
                }

                val sourceFile = noteDataSource.getNoteFile(sourcePath, note.id)
                if (!sourceFile.exists()) {
                    throw IOException("Source note file not found: ${note.id}")
                }

                // Если ни один блокнот не защищён — просто перемещаем файл (быстрый путь)
                if (!sourceProtected && !targetProtected) {
                    val targetDir = if (targetNotebookPath != null) {
                        File(noteDataSource.baseDir, targetNotebookPath).apply {
                            noteDataSource.createDirectory(this)
                        }
                    } else {
                        noteDataSource.baseDir
                    }
                    val targetFile = File(targetDir, "${note.id}.txt")
                    if (targetFile.exists()) {
                        throw IOException("File already exists in target directory")
                    }
                    noteDataSource.moveFile(sourceFile, targetFile)
                    // Дату сохраняем (если нужно) — moveFile сохраняет метаданные? Лучше явно установить
                    if (note.modifiedAt > 0) {
                        noteDataSource.setNoteDate(targetFile, note.modifiedAt)
                    }
                    return@withContext
                }

                // Читаем сырое содержимое (возможно, зашифрованное)
                val rawContent = noteDataSource.readNoteContent(sourceFile)

                // Расшифровываем, если исходный защищён
                val plainContent = if (sourceProtected) {
                    encryptionRepository.decryptNote(sourcePath, rawContent)
                } else {
                    rawContent
                }

                // Шифруем, если целевой защищён
                val finalContent = if (targetProtected) {
                    encryptionRepository.encryptNote(targetNotebookPath, plainContent)
                } else {
                    plainContent
                }

                // Создаём целевую директорию
                val targetDir = if (targetNotebookPath != null) {
                    File(noteDataSource.baseDir, targetNotebookPath).apply {
                        noteDataSource.createDirectory(this)
                    }
                } else {
                    noteDataSource.baseDir
                }

                val targetFile = File(targetDir, "${note.id}.txt")
                if (targetFile.exists()) {
                    throw IOException("File already exists in target directory")
                }

                // Записываем содержимое в целевой файл
                noteDataSource.writeNoteContent(targetFile, finalContent)

                // Восстанавливаем дату последнего изменения
                if (note.modifiedAt > 0) {
                    noteDataSource.setNoteDate(targetFile, note.modifiedAt)
                } else {
                    // Если дата не задана, ставим текущую
                    noteDataSource.setNoteDate(targetFile, System.currentTimeMillis())
                }

                // Удаляем исходный файл
                noteDataSource.deleteNote(sourcePath, note.id)

            } catch (e: AuthenticationRequiredException) {
                // Пробрасываем выше для обработки в ViewModel
                throw e
            } catch (e: Exception) {
                Log.e("NoteRepository", "Error moving note: ${e.message}")
                throw IOException("Error moving note: ${e.message}")
            }
        }
    }

    override suspend fun renameNote(note: Note, newName: String): Note {
        return withContext(Dispatchers.IO) {
            try {
                val newId = if (newName == "") generateNoteId() else sanitizeFileName(newName)

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
                note.copy(id = newId, title = newName)
                //getNote(newId, note.notebookPath) так нельзя потому что будет ошибка если ключ истек

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

        allNotes.addAll(getNotesList(null))
        notebooks.forEach { notebook ->
            allNotes.addAll(getNotesList(notebook.path))
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

    override suspend fun findNotes(
        notebookPath: String?,
        query: String,
        notebooks: List<Notebook>
    ): List<NoteFound> = withContext(Dispatchers.IO) {
        val previewLen = 50
        val lowerQuery = query.lowercase()
        val result = mutableListOf<NoteFound>()

        // Определяем список путей для обхода
        val pathsToSearch = if (notebookPath != null) {
            listOf(notebookPath)   // только указанный блокнот
        } else {
            // общий поиск: корень + все переданные блокноты
            val allPaths = mutableListOf<String?>(null)
            allPaths.addAll(notebooks.map { it.path })
            allPaths
        }

        for (path in pathsToSearch) {
            val dir =
                if (path != null) File(noteDataSource.baseDir, path) else noteDataSource.baseDir
            val files = noteDataSource.listFilesInDirectory(dir)
                ?.filter { it.isFile && it.name.endsWith(".txt") }
                ?: continue

            val isProtected = path != null && encryptionRepository.hasKey(path)
            val isUnlocked = isProtected && encryptionRepository.isUnlocked(path)

            for (file in files) {
                val id = file.nameWithoutExtension
                val title = if (id.startsWith(FILE_NAME_PREFIX)) "" else id

                // Поиск по названию (всегда)
                if (title.lowercase().contains(lowerQuery) || id.lowercase().contains(lowerQuery)) {
                    result.add(
                        NoteFound(
                            query = query,
                            id = id,
                            title = title,
                            foundedInTitle = true,
                            contentSearchPreview = null,
                            contentPosition = null,
                            modifiedAt = file.lastModified(),
                            notebookPath = path
                        )
                    )
                }

                // Поиск по содержимому (только если можно прочитать)
                val canReadContent = !isProtected || (isProtected && isUnlocked) || true ///
                if (canReadContent) {
                    try {
                        val rawContent = noteDataSource.readNoteContent(file)
                        val content = if (isProtected) {
                            encryptionRepository.decryptNote(path!!, rawContent)
                        } else {
                            rawContent
                        }

                        // Находим все позиции вхождения (регистронезависимо)
                        val lowerContent = content.lowercase()
                        val positions = mutableListOf<Int>()
                        var start = lowerContent.indexOf(lowerQuery)
                        while (start != -1) {
                            positions.add(start)
                            start = lowerContent.indexOf(lowerQuery, start + 1)
                        }

                        // Для каждой позиции строим фрагмент и группируем по содержанию
                        val fragmentMap =
                            mutableMapOf<String, Pair<Int, Int>>() // contentPart -> (startOffset, firstPosition)
                        for (pos in positions) {
                            val (fragment, offset) = extractFragment(content, pos, query.length, previewLen)
                            // Если такой фрагмент уже есть, не добавляем дубль
                            if (!fragmentMap.containsKey(fragment)) {
                                fragmentMap[fragment] = offset to pos
                            }
                        }

                        // Добавляем результаты
                        for ((fragment, pair) in fragmentMap) {
                            val (offset, firstPos) = pair
                            result.add(
                                NoteFound(
                                    query = query,
                                    id = id,
                                    title = title,
                                    foundedInTitle = false,
                                    contentSearchPreview = fragment,
                                    //contentPreviewOffset = offset,
                                    contentPosition = firstPos,
                                    modifiedAt = file.lastModified(),
                                    notebookPath = path
                                )
                            )
                        }

                    } catch (e: AuthenticationRequiredException) {
                        // Ключ внезапно стал недоступен – пропускаем чтение содержимого
                        // (поиск по названию уже сделан)
                        throw e //выбрасываем ошибку чтобы пользователю сказать
                    } catch (e: Exception) {
                        // Логируем, но не прерываем общий поиск
                        Log.w(
                            "NoteRepository",
                            "Failed to read content for ${file.name}: ${e.message}"
                        )
                    }
                }
            }
        }

        // Сортируем по дате изменения (сначала новые)
        result.sortedByDescending { it.modifiedAt }
    }

    // Вспомогательная функция для извлечения фрагмента
    private fun extractFragment(
        text: String,
        position: Int,
        queryLen: Int,
        maxLen: Int
    ): Pair<String, Int> {
        val textLen = text.length
        if (textLen <= maxLen) return Pair(text, 0)

        // Вычисляем начало фрагмента так, чтобы запрос оказался примерно в центре
        var start = position - (maxLen - queryLen) / 2
        start = max(0, min(start, textLen - maxLen))
        val end = min(start + maxLen, textLen)
        val fragment = text.substring(start, end)
        return Pair(fragment, start)
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