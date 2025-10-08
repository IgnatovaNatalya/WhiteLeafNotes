package com.example.whiteleafnotes.domain.use_case

import android.net.Uri
import com.example.whiteleafnotes.domain.repository.ExportRepository
import com.example.whiteleafnotes.domain.repository.NotesRepository
import com.example.whiteleafnotes.domain.repository.NotebookRepository

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
