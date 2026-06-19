package ru.whiteleaf.notes.domain.use_case

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository

class RenameNotebookUseCase(
    private val notebookRepository: NotebookRepository,
    private val notesRepository: NotesRepository,
    private val encryptionRepository: EncryptionRepository
) {
    suspend operator fun invoke(notebookPath: String, newName: String) {

        val isProtected = encryptionRepository.hasKey(notebookPath)

        if (isProtected) {
            notesRepository.decryptAllNotes(notebookPath)
            encryptionRepository.deleteKeyForNotebook(notebookPath)
        }

        notebookRepository.renameNotebook(notebookPath, newName)

        if (isProtected) {
            encryptionRepository.createKeyForNotebook(newName)
            notesRepository.encryptAllNotes(newName)}
    }
}