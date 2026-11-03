package ru.whiteleaf.notes.domain.use_case.notes

import android.content.Context
import android.net.Uri
import ru.whiteleaf.notes.common.utils.ImageStorageUtils
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.NotesRepository

class InsertImageUseCase(
    private val context: Context,
    private val notesRepository: NotesRepository
) {
    suspend operator fun invoke(note: Note, imageUri: Uri): Note {
        val filename = ImageStorageUtils.saveImageFromUri(context, imageUri, note.id, note.notebookPath)
            ?: throw IllegalArgumentException("Failed to save image")

        val marker = "[IMG:$filename]"
        val updatedContent = note.content + "\n$marker"

        return note.copy(content = updatedContent)
    }
}
