package ru.whiteleaf.notes.data.datasource

import android.content.Context
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
import ru.whiteleaf.notes.data.config.NotebookConfigManager
import java.io.File


class FileNoteDataSource(
    private val context: Context,
    private val configManager: NotebookConfigManager,
    private val encryptionManager: EncryptionManager
) {

    val baseDir: File by lazy {
        File(context.filesDir, DEFAULT_DIR).apply {
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

    // Удаление заметки
    fun deleteNote(notebookPath: String, noteId: String): Boolean {
        val file = getNoteFile(notebookPath, noteId)
        return if (file.exists()) {
            file.delete()
        } else {
            false
        }
    }

    // Проверка существования файла
    fun existsNote(notebookPath: String, noteId: String): Boolean {
        val file = getNoteFile(notebookPath, noteId)
        return file.exists()
    }

    // Чтение содержимого файла
    //fun readNoteContent(file: File): String {
    //return file.readText()
    //}

    fun readNoteContent(file: File, notebookPath: String): String {
        val content = file.readText()

        return if (configManager.isNotebookProtected(notebookPath) && encryptionManager != null) {
            val keyAlias = configManager.getKeyAliasForNotebook(notebookPath)!!
            // Просто пытаемся расшифровать - если нужна биометрия, будет исключение
            encryptionManager.decryptContent(content, keyAlias)
        } else {
            content
        }
    }

    // Запись содержимого в файл
    //fun writeNoteContent(file: File, content: String) {
    //file.writeText(content)
    //}

    fun writeNoteContent(file: File, content: String, notebookPath: String) {
        val contentToWrite =
            if (configManager.isNotebookProtected(notebookPath) && encryptionManager != null) {
                val keyAlias = configManager.getKeyAliasForNotebook(notebookPath)!!
                encryptionManager.encryptContent(content, keyAlias)
            } else {
                content
            }

        file.writeText(contentToWrite)
    }


    // Установка времени последнего изменения
    fun setFileLastModified(file: File, timestamp: Long) {
        file.setLastModified(timestamp)
    }

    // Получение списка файлов в директории
    fun listFilesInDirectory(directory: File): Array<File>? {
        return directory.listFiles()
    }

    // Создание директории
    fun createDirectory(directory: File): Boolean {
        return directory.mkdirs()
    }

    // Перемещение/переименование файла
    fun moveFile(source: File, target: File): Boolean {
        return if (source.renameTo(target)) {
            true
        } else {
            // Если renameTo не сработал, копируем и удаляем оригинал
            try {
                source.copyTo(target, overwrite = true)
                source.delete()
                true
            } catch (_: Exception) {
                false
            }
        }
    }
}
