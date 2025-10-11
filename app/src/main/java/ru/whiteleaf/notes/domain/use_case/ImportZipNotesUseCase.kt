package ru.whiteleaf.notes.domain.use_case

import android.content.Context
import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class ImportZipNotesUseCase(
    private val notesRepository: NotesRepository,
    private val notebookRepository: NotebookRepository,
    private val context: Context
) {
    suspend fun execute(zipFileUri: Uri): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val tempDir = createTempDir(context)
            try {
                unzip(zipFileUri, tempDir, context)
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
        val notebookName = directory.name
        var notebookPath = notebookName
        var counter = 1

        while (notebookRepository.notebookExist(notebookPath))
            notebookPath = "${notebookName}_${counter++}"

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

    private fun unzip(zipFileUri: Uri, targetDir: File, context: Context) {
        context.contentResolver.openInputStream(zipFileUri)?.use { inputStream ->
            ZipInputStream(inputStream).use { zipStream ->
                var entry: ZipEntry? = zipStream.nextEntry

                while (entry != null) {
                    val file = File(targetDir, entry.name)

                    if (entry.isDirectory) {
                        file.mkdirs()
                    } else {
                        file.parentFile?.mkdirs()

                        file.outputStream().use { outputStream ->
                            zipStream.copyTo(outputStream)
                        }
                    }

                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        } ?: throw IllegalArgumentException("Cannot open ZIP file")
    }
}