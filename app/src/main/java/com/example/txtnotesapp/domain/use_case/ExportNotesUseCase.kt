package com.example.txtnotesapp.domain.use_case

import android.net.Uri
import com.example.txtnotesapp.domain.repository.NotesRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository

class ExportNotesUseCase(
    private val noteRepository: NotesRepository,
    private val notebookRepository: NotebookRepository
) {
    suspend operator fun invoke(password: String? = null): Result<Uri> {
        return try {
            val notes = noteRepository.getAllNotes(notebookRepository.getNotebooks())
            val notebooks = notebookRepository.getNotebooks()
            val result = noteRepository.exportToZip(notes, notebooks, password)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}