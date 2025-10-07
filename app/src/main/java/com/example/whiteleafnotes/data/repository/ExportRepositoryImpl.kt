package com.example.whiteleafnotes.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.whiteleafnotes.common.AppConstants.DEFAULT_DIR
import com.example.whiteleafnotes.common.AppConstants.EXPORT_ZIP_PREFIX
import com.example.whiteleafnotes.data.datasource.FileNoteDataSource
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.repository.ExportRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

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
                val zipFile = createZipFile(tempDir, password)
                saveToExternalStorage(zipFile)
            } finally {
                // Очищаем временные файлы
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
            val sourceFile = fileNoteDataSource.getNoteFile(note.notebookPath ?: "", note.id)
            val targetDir = if (note.notebookPath != null) {
                File(tempDir, note.notebookPath)
            } else {
                tempDir
            }
            val targetFile = File(targetDir, "${note.id}.txt")

            if (sourceFile.exists()) {
                sourceFile.copyTo(targetFile, overwrite = true)
                targetFile.setLastModified(note.modifiedAt)
            }
        }
    }

    private fun createZipFile(
        tempDir: File,
        password: String? = null  //todo сделать с паролем
    ): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val zipFile = File(context.cacheDir, "$EXPORT_ZIP_PREFIX$timestamp.zip")

        ZipOutputStream(FileOutputStream(zipFile)).use { zipOut ->
            tempDir.walk().forEach { file ->
                if (file.isFile) {
                    val relativePath = file.relativeTo(tempDir).path
                    val zipEntry = ZipEntry(relativePath).apply {
                        time = file.lastModified()
                    }
                    zipOut.putNextEntry(zipEntry)
                    file.inputStream().use { it.copyTo(zipOut) }
                    zipOut.closeEntry()
                }
            }
        }
        return zipFile
    }

    private fun saveToExternalStorage(zipFile: File): Uri {

        val contentValues = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, zipFile.name)
            put(MediaStore.Downloads.MIME_TYPE, "application/zip")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/$DEFAULT_DIR")
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