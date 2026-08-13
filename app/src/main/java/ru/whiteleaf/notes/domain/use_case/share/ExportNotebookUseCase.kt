package ru.whiteleaf.notes.domain.use_case.share

import android.content.Context
import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class ExportNotebookUseCase(
    private val noteRepository: NotesRepository,
    private val notebookRepository: NotebookRepository,
    private val exportRepository: ExportRepository,
    private val encryptionRepository: EncryptionRepository
) {
    suspend operator fun invoke(
        context: Context,
        notebookPath: String,
        password: String? = null,
        progressCallback: ExportProgressCallback? = null
    ): Result<Uri?> {

        val notebook = notebookRepository.getNotebookByPath(notebookPath) ?: return Result.failure(
            IllegalArgumentException("Notebook not found")
        )

        val notesToExport = mutableListOf<Note>()

        progressCallback?.onNotebookExportStarted(notebookPath)

        val isProtected = encryptionRepository.hasKey(notebookPath)

        val unlocked =
            if (isProtected) encryptionRepository.unlockNotebook(
                notebookPath,
                context,
                "Записная книжка защищена",
                "Для экспорта"
            ) else true

        return if (unlocked) {
            notesToExport.addAll(noteRepository.getNotes(notebookPath))

//            println("DEBUG: ExportNotebookUseCase: added notes to export: ${notesToExport.size} notes")
//            notesToExport.forEach { note->println("DEBUG: ExportNotebookUseCase: note ${note.printDebug()} ")}
            Result.success(
                exportRepository.createExportZip(notesToExport, listOf(notebook), password)
            )
        } else {
            Result.failure(exception = AuthenticationRequiredException("Authentication error"))
        }
    }
}


//    (
//    private val notebookRepository: NotebookRepository,
//    private val noteRepository: NotesRepository,
//    private val exportRepository: ExportRepository
//) {
//    suspend operator fun invoke(notebookPath: String, password: String? = null): Result<Uri> {
//        return try {
//            val notebook = notebookRepository.getNotebookByPath(notebookPath)
//            val notes = noteRepository.getNotesList(notebook?.path)
//            val result = exportRepository.createExportZip(notes, listOf(notebook!!), password)
//            Result.success(result)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//}
