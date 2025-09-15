package com.example.txtnotesapp.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import com.example.txtnotesapp.data.datasource.FileNoteDataSource
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.ExternalRepository
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

class ExternalRepositoryImpl(
    private val context: Context,
    private val fileNoteDataSource: FileNoteDataSource
) : ExternalRepository {

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
        notebooks: List<Notebook>) {
        // Создаем папки для записных книжек
        notebooks.forEach { notebook ->
            val notebookDir = File(tempDir, notebook.path).apply { mkdirs() }

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
        val zipFile = File(context.cacheDir, "txtnotes_export_$timestamp.zip")

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
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IOException("Не удалось создать файл для экспорта")

        resolver.openOutputStream(uri)?.use { output ->
            zipFile.inputStream().use { input ->
                input.copyTo(output)
            }
        }

        // Удаляем временный zip файл
        //zipFile.delete()

        return uri
    }

}