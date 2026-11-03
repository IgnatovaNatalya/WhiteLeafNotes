package ru.whiteleaf.notes.common.utils

import android.content.Context
import android.net.Uri
import ru.whiteleaf.notes.common.AppConstants
import ru.whiteleaf.notes.common.utils.FileUtils.generateImageId
import java.io.File

object ImageStorageUtils {

    private const val IMAGES_DIR_NAME = "images"

    /**
     * Путь к директории images внутри блокнота: baseDir/notebookPath/images/
     */
    fun getImageDir(context: Context, notebookPath: String?): File {
        val baseDir = File(context.filesDir, AppConstants.DEFAULT_DIR)
        return if (notebookPath != null && notebookPath.isNotEmpty()) {
            File(baseDir, "$notebookPath/$IMAGES_DIR_NAME").apply {
                if (!exists()) mkdirs()
            }
        } else {
            File(baseDir, IMAGES_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
        }
    }

    /**
     * Сохраняет изображение из URI в директорию noteId_images/
     * Возвращает filename: "noteId_a1b2c3d4.jpg"
     */
    fun saveImageFromUri(context: Context, uri: Uri, noteId: String, notebookPath: String?): String? {
        val imageDir = getImageDir(context, notebookPath)
        val imageId = generateImageId()
        val extension = getImageExtension(context, uri) ?: "jpg"
        val filename = "${noteId}_$imageId.$extension"
        val targetFile = File(imageDir, filename)

        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            filename
        } catch (e: Exception) {
            println("DEBUG: ImageStorageUtils: Failed to save image: ${e.message}")
            null
        }
    }

    /**
     * Получает расширение файла из URI
     */
    private fun getImageExtension(context: Context, uri: Uri): String? {
        val contentResolver = context.contentResolver
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "image/png" -> "png"
            "image/gif" -> "gif"
            "image/webp" -> "webp"
            else -> {
                // Fallback: try to get from URI path
                val path = uri.path
                path?.let { p ->
                    val dotIndex = p.lastIndexOf('.')
                    if (dotIndex > 0) p.substring(dotIndex + 1).lowercase() else "jpg"
                } ?: "jpg"
            }
        }
    }

    /**
     * Возвращает File для конкретного изображения
     */
    fun getImageFile(context: Context, noteId: String, filename: String, notebookPath: String?): File {
        val imageDir = getImageDir(context, notebookPath)
        return File(imageDir, filename)
    }

    /**
     * Находит все файлы noteId_*.* и удаляет
     * Возвращает количество удалённых файлов
     */
    fun deleteNoteImages(context: Context, noteId: String, notebookPath: String?): Int {
        val imageDir = getImageDir(context, notebookPath)
        if (!imageDir.exists()) return 0

        var deletedCount = 0
        imageDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("${noteId}_")) {
                if (file.delete()) deletedCount++
            }
        }
        return deletedCount
    }

    /**
     * Переименовывает ВСЕ файлы noteId_old_*.* → noteId_new_*.*
     * Возвращает Map<oldFilename, newFilename> для обновления маркеров в контенте
     */
    fun renameNoteImages(context: Context, oldNoteId: String, newNoteId: String, notebookPath: String?): Map<String, String> {
        val imageDir = getImageDir(context, notebookPath)
        if (!imageDir.exists()) return emptyMap()

        val renameMap = mutableMapOf<String, String>()

        imageDir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith("${oldNoteId}_")) {
                // Извлекаем suffix: oldNoteId_a1b2c3d4.jpg → _a1b2c3d4.jpg
                val suffix = file.name.substring(oldNoteId.length)
                val newFilename = "$newNoteId$suffix"
                val newFile = File(imageDir, newFilename)

                if (file.renameTo(newFile)) {
                    renameMap[file.name] = newFilename
                }
            }
        }
        return renameMap
    }

    /**
     * Перемещает ВСЕ файлы noteId_*.* из sourcePath/images/ в targetPath/images/
     */
    fun moveNoteImages(context: Context, noteId: String, sourcePath: String?, targetPath: String?): Boolean {
        val sourceDir = getImageDir(context, sourcePath)
        val targetDir = getImageDir(context, targetPath)

        if (!sourceDir.exists()) return true // нечего перемещать — успешно

        val files = sourceDir.listFiles() ?: return false
        var success = true

        files.forEach { file ->
            if (file.isFile && file.name.startsWith("${noteId}_")) {
                val targetFile = File(targetDir, file.name)
                success = success && file.renameTo(targetFile)
            }
        }

        // Удаляем пустую source директорию
        if (success && sourceDir.listFiles()?.isEmpty() == true) {
            sourceDir.delete()
        }

        return success
    }

    /**
     * Парсит [IMG:noteId_uuid.ext] из контента
     */
    fun extractImageFilenames(content: String): List<String> {
        val pattern = Regex(AppConstants.IMAGE_MARKER_PATTERN)
        return pattern.findAll(content)
            .mapNotNull { matchResult -> matchResult.groupValues.getOrNull(1) }
            .toList()
    }

    /**
     * Парсит noteId из filename (noteId_uuid.ext → noteId)
     * Формат: noteId_suffix.ext, где suffix = _uuid.ext
     */
    fun extractNoteIdFromFilename(filename: String): String {
        // Убираем расширение
        val nameWithoutExt = filename.substringBeforeLast('.')
        // Находим первый '_' после noteId
        val underscoreIndex = nameWithoutExt.indexOf('_')
        return if (underscoreIndex > 0) {
            nameWithoutExt.substring(0, underscoreIndex)
        } else {
            nameWithoutExt
        }
    }

    /**
     * Находит orphaned файлы (есть на диске, но нет в контенте ни одной заметки)
     */
    fun cleanupOrphans(context: Context, allNoteContents: Map<String, String>): List<File> {
        val allReferencedImages = mutableSetOf<String>()
        allNoteContents.values.forEach { content ->
            extractImageFilenames(content).forEach { allReferencedImages.add(it) }
        }

        val orphans = mutableListOf<File>()
        // Сканируем все директории images
        val baseDir = File(context.filesDir, AppConstants.DEFAULT_DIR)
        baseDir.walk()
            .filter { it.name == IMAGES_DIR_NAME && it.isDirectory }
            .forEach { imagesDir ->
                imagesDir.listFiles()?.forEach { file ->
                    if (file.isFile && !allReferencedImages.contains(file.name)) {
                        orphans.add(file)
                    }
                }
            }
        return orphans
    }

    /**
     * Обновляет маркеры в контенте: oldMarker → newMarker
     */
    fun updateImageMarkersInContent(content: String, renameMap: Map<String, String>): String {
        var updatedContent = content
        renameMap.forEach { (oldMarker, newMarker) ->
            updatedContent = updatedContent.replace(
                "$AppConstants.IMAGE_MARKER_PREFIX$oldMarker$AppConstants.IMAGE_MARKER_SUFFIX",
                "$AppConstants.IMAGE_MARKER_PREFIX$newMarker$AppConstants.IMAGE_MARKER_SUFFIX"
            )
        }
        return updatedContent
    }
}
