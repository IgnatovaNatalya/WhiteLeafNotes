package ru.whiteleaf.notes.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
import ru.whiteleaf.notes.common.AppConstants.EXPORT_ZIP_PREFIX
import ru.whiteleaf.notes.data.datasource.FileNoteDataSource
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import net.lingala.zip4j.ZipFile
import net.lingala.zip4j.model.ZipParameters
import net.lingala.zip4j.model.enums.AesKeyStrength
import net.lingala.zip4j.model.enums.CompressionLevel
import net.lingala.zip4j.model.enums.EncryptionMethod

class ExportRepositoryImpl(
    private val context: Context,
    private val fileNoteDataSource: FileNoteDataSource
) : ExportRepository {

    override suspend fun createExportZip(
        notes: List<Note>,
        notebooks: List<Notebook>,
        password: String?
    ): Uri {
        return withContext(Dispatchers.IO) {
            val tempDir = File(context.cacheDir, "export_temp").apply {
                deleteRecursively()
                mkdirs()
            }

            try {
                createExportStructure(tempDir, notes, notebooks)
                val zipFile = createPasswordProtectedZip(tempDir, password)
                saveToExternalStorage(zipFile)

            } finally {
                tempDir.deleteRecursively()
            }
        }
    }

    private fun createExportStructure(
        tempDir: File,
        notes: List<Note>,
        notebooks: List<Notebook>
    ) {
        // Создаем папки для записных книжек
        notebooks.forEach { notebook ->
            File(tempDir, notebook.path).apply { mkdirs() }
        }

        // Копируем все заметки
        notes.forEach { note ->
            val targetDir =
                if (note.notebookPath != null) File(tempDir, note.notebookPath) else tempDir
            val targetFile = File(targetDir, "${note.title}.txt")

            fileNoteDataSource.writeNoteContent(targetFile, note.content)
            targetFile.setLastModified(note.modifiedAt)
        }
    }

    fun createPasswordProtectedZip(tempDir: File, password: String? = null): File {

        require(tempDir.exists() && tempDir.isDirectory) {
            "$tempDir должен быть существующей директорией"
        }

        val timestamp = SimpleDateFormat("dd_MM_yyyy_HHmmss", Locale.getDefault()).format(Date())
        val outputZipFile = File(context.cacheDir, "$EXPORT_ZIP_PREFIX$timestamp.zip")

        val zipFile = if (password != null) {
            ZipFile(outputZipFile, password.toCharArray())
        } else {
            ZipFile(outputZipFile)
        }

        val zipParameters = ZipParameters().apply {
            compressionLevel = CompressionLevel.NORMAL
            if (password != null) {
                encryptionMethod = EncryptionMethod.AES
                aesKeyStrength = AesKeyStrength.KEY_STRENGTH_256
                isEncryptFiles = true
            }
        }

        tempDir.walk().forEach { file ->
            if (file.isFile) {
                zipParameters.fileNameInZip = file.relativeTo(tempDir).path.replace('\\', '/')
                zipFile.addFile(file, zipParameters)
            }
        }
        return outputZipFile
    }

    private fun saveToExternalStorage(zipFile: File): Uri {

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(
                MediaStore.Downloads.RELATIVE_PATH,
                "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_DIR"
            )
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Не удалось создать файл для экспорта")

        resolver.openOutputStream(uri)?.use { output ->
            zipFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }
        return uri
    }
}