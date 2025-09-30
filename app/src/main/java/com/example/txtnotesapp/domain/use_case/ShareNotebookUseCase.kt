package com.example.txtnotesapp.domain.use_case

import android.net.Uri
import com.example.txtnotesapp.domain.repository.ExternalRepository
import com.example.txtnotesapp.domain.repository.NotesRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository

class ShareNotebookUseCase(
    private val notebookRepository: NotebookRepository,
    private val noteRepository: NotesRepository,
    private val externalRepository: ExternalRepository
) {
    suspend operator fun invoke(notebookPath: String, password: String? = null): Result<Uri> {
        return try {
            val notebook = notebookRepository.getNotebookByPath(notebookPath)
            val notes = noteRepository.getNotes(notebook?.path)
            val result = externalRepository.createExportZip(notes, listOf(notebook!!), password)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
