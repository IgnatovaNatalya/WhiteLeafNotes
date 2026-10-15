package ru.whiteleaf.notes.domain.use_case.share

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import java.io.File
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.exception.ZipException
import java.nio.charset.Charset

class ImportZipNotesUseCase(
    private val notesRepository: NotesRepository,
    private val notebookRepository: NotebookRepository,
    private val context: Context
) {
    suspend fun execute(zipFileUri: Uri, password: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) { //todo передавать сюда пароль для распаковки архива
            try {
                val tempDir = createTempDir(context)
                try {
                    unzip(zipFileUri, tempDir, context, password)
                    processExtractedFiles(tempDir)
                    Result.success(Unit)
                } finally {
                    tempDir.deleteRecursively()
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun createTempDir(context: Context): File {
        return File(context.cacheDir, "temp_import_${System.currentTimeMillis()}").apply {
            mkdirs()
        }
    }

    private suspend fun processExtractedFiles(rootDir: File) {
        rootDir.listFiles()?.forEach { file ->
            when {
                file.isDirectory -> processNotebookDirectory(file)
                file.isFile && isTxtFile(file) -> processNoteFile(file, null)
            }
        }
    }

    private suspend fun processNotebookDirectory(directory: File) {
        var notebookPath = directory.name
        var counter = 1

        while (notebookRepository.notebookExist(notebookPath))
            notebookPath = "${notebookPath}_${counter++}"

        notebookRepository.createNotebook(notebookPath)

        directory.listFiles()?.forEach { file ->
            if (file.isFile && isTxtFile(file)) {
                processNoteFile(file, notebookPath)
            }
        }
    }

    private suspend fun processNoteFile(file: File, notebookPath: String?) {

        val content = file.readText()

        val baseId = file.nameWithoutExtension
        var counter = 1
        var noteId = baseId

        while (notesRepository.existsNote(notebookPath ?: "", noteId))
            noteId = "${baseId}_${counter++}"

        val note = Note(
            id = noteId,
            title = "",
            content = content,
            notebookPath = notebookPath,
            modifiedAt = file.lastModified()
        )

        notesRepository.saveNote(note)
    }

    private fun isTxtFile(file: File): Boolean {
        return file.extension.equals("txt", ignoreCase = true)
    }

    private fun unzip(
        zipFileUri: Uri,
        targetDir: File,
        context: Context,
        password: String?
    ) {
        // 1. Копируем содержимое Uri во временный файл, т.к. zip4j.ZipFile требует File
        val tempZipFile = File(context.cacheDir, "temp_import_${System.currentTimeMillis()}.zip")
        try {
            context.contentResolver.openInputStream(zipFileUri)?.use { inputStream ->
                tempZipFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: throw IllegalArgumentException("Cannot open ZIP file")

            // 2. Используем zip4j для распаковки

            //сначала узнаем используется ли UTF-8
            val zipFileForEncoding = if (password.isNullOrEmpty()) ZipFile(tempZipFile)
            else ZipFile(tempZipFile, password.toCharArray())

            val isUtf8Encoded = zipFileForEncoding.fileHeaders.firstOrNull()?.isFileNameUTF8Encoded ?: false
            println("DEBUG: ImportNotesUseCase: isUtf8Encoded=$isUtf8Encoded")

            // сама распаковка
            val zipFile = if (password.isNullOrEmpty()) ZipFile(tempZipFile)
            else ZipFile(tempZipFile, password.toCharArray())

            if (!isUtf8Encoded) zipFile.setCharset(Charset.forName("cp866"))

            zipFile.extractAll(targetDir.absolutePath)
        } catch (e: ZipException) {
            // Оборачиваем ошибку zip4j в понятное исключение
            println("DEBUG: ImportNotesUseCase: Failed to extract ZIP: ${e.message}")
            throw IllegalArgumentException("Failed to extract ZIP: ${e.message}", e)
        } finally {
            // 3. Удаляем временный zip-файл
            tempZipFile.delete()
        }
    }
}