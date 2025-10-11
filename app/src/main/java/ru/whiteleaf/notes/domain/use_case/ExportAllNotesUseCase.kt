package ru.whiteleaf.notes.domain.use_case

import android.net.Uri
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class ExportAllNotesUseCase(
    private val noteRepository: NotesRepository,
    private val notebookRepository: NotebookRepository,
    private val exportRepository: ExportRepository
) {
    suspend operator fun invoke(password: String? = null): Result<Uri> {
        return try {
            val notes = noteRepository.getAllNotes(notebookRepository.getNotebooks())
            val notebooks = notebookRepository.getNotebooks()
            val result = exportRepository.createExportZip(notes, notebooks, password)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}