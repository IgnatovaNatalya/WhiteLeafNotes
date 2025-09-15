package com.example.txtnotesapp.domain.use_case

import android.net.Uri
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.repository.ExternalRepository
import com.example.txtnotesapp.domain.repository.NotesRepository

class ExportNotebookUseCase(
    private val noteRepository: NotesRepository,
    private val externalRepository: ExternalRepository
) {
    suspend operator fun invoke(notebook:Notebook, password: String? = null): Result<Uri> {
        return try {
            val notes = noteRepository.getNotes(notebook.path)
            val result = externalRepository.createExportZip(notes, listOf(notebook), password)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}