package ru.whiteleaf.notes.domain.use_case

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import ru.whiteleaf.notes.domain.model.SharedContent
import ru.whiteleaf.notes.domain.model.SharedContentResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class GetSharedContentUseCase(
    private val contentResolver: ContentResolver
) {
    suspend fun execute(intent: Intent): SharedContentResult<SharedContent> {
        return when {
            intent.action != Intent.ACTION_SEND ->
                SharedContentResult.Error("Invalid action: ${intent.action}")

            intent.hasExtra(Intent.EXTRA_STREAM) -> {
                val uri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)!!
                loadFromUri(uri)
            }

            intent.hasExtra(Intent.EXTRA_TEXT) -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT)!!
                SharedContentResult.Success(
                    SharedContent.TextContent(
                        text.substringBefore("http").trim(' ', '\t', '\n', '\r', '"')
                    )
                )
            }

            else -> SharedContentResult.Error("No content found in intent")
        }
    }

    private suspend fun loadFromUri(uri: Uri): SharedContentResult<SharedContent> {
        return try {
            withContext(Dispatchers.IO) {
                contentResolver.openInputStream(uri)?.use { stream ->
                    val fileName = getFileName(uri)
                    val content = stream.bufferedReader().use { it.readText() }

                    SharedContentResult.Success(
                        SharedContent.FileContent(
                            name = fileName?.removeSuffix(".txt") ?: "Без названия",
                            text = content
                        )
                    )
                } ?: SharedContentResult.Error("Cannot open file stream")
            }
        } catch (e: SecurityException) {
            SharedContentResult.Error("Permission denied: ${e.message}")
        } catch (e: IOException) {
            SharedContentResult.Error("IO error: ${e.message}")
        } catch (e: Exception) {
            SharedContentResult.Error("Unexpected error: ${e.message}")
        }
    }

    @SuppressLint("Range")
    private fun getFileName(uri: Uri): String? {
        return when (uri.scheme) {
            "content" -> {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(
                            cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        )
                    } else null
                }
            }

            "file" -> uri.lastPathSegment
            else -> null
        }
    }
}