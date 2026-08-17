package ru.whiteleaf.notes.domain.use_case.share

import android.content.Context
import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository

class ExportAllNotesUseCase(
    private val noteRepository: NotesRepository,
    private val notebookRepository: NotebookRepository,
    private val exportRepository: ExportRepository,
    private val encryptionRepository: EncryptionRepository
) {
    suspend operator fun invoke(
        context: Context,
        exportEncrypted: Boolean,
        password: String? = null,
        progressCallback: ExportProgressCallback? = null
    ): Result<Uri> {
        return try {
            val notesToExport = mutableListOf<Note>()
            val noteBooksToExport = mutableListOf<Notebook>()
            val notebooks = notebookRepository.getNotebooks()

            for (notebook in notebooks) {
                progressCallback?.onNotebookExportStarted(notebook.path)
                if (encryptionRepository.hasKey(notebook.path)) {
                    //если зашифрованная
                    if (exportEncrypted) {
                        val unlocked = encryptionRepository.unlockNotebook(
                            notebook.path,
                            context,
                            "Экспорт защищенной книжки «${notebook.path}»",
                            "Для экспорта"
                        )
                        if (unlocked) {
                            notesToExport.addAll(noteRepository.getNotes(notebook.path))
                            noteBooksToExport.add(notebook)
                            println("DEBUG: ExportAllNotesUseCase: Unlock success protected notebook ${notebook.path} added to export")
                        } else {
                            println("DEBUG: ExportAllNotesUseCase: Unlock failed, skip protected notebook ${notebook.path}")
                        }
                    }

                } else {
                    //если не зашифрованная
                    notesToExport.addAll(noteRepository.getNotesList(notebook.path))
                    noteBooksToExport.add(notebook)
                    println("DEBUG: ExportAllNotesUseCase: Open notebook ${notebook.path} added to export")
                }
            }

            val result =
                exportRepository.createExportZip(notesToExport, noteBooksToExport, password)

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

interface ExportProgressCallback {
    fun onNotebookExportStarted(notebookName: String)
}