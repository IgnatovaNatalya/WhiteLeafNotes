package com.example.txtnotesapp.domain.use_case

import com.example.txtnotesapp.domain.repository.NoteRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository

class ExportNotesUseCase(
    private val noteRepository: NoteRepository,
    private val notebookRepository: NotebookRepository
) {
    suspend operator fun invoke(password: String? = null): Result<Uri> {
        return try {
            val notes = noteRepository.getNotes()
            val notebooks = notebookRepository.getNotebooks()
            val result = noteRepository.exportToZip(notes, notebooks, password)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}