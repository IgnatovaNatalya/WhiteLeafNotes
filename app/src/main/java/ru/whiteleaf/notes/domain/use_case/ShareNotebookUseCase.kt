package ru.whiteleaf.notes.domain.use_case

import android.net.Uri
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class ShareNotebookUseCase(
    private val notebookRepository: NotebookRepository,
    private val noteRepository: NotesRepository,
    private val exportRepository: ExportRepository
) {
    suspend operator fun invoke(notebookPath: String, password: String? = null): Result<Uri> {
        return try {
            val notebook = notebookRepository.getNotebookByPath(notebookPath)
            val notes = noteRepository.getNotes(notebook?.path)
            val result = exportRepository.createExportZip(notes, listOf(notebook!!), password)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
