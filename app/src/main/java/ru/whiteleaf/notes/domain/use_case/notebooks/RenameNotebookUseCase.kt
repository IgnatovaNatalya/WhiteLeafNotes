package ru.whiteleaf.notes.domain.use_case.notebooks

import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

class RenameNotebookUseCase(
    private val notebookRepository: NotebookRepository,
    private val notesRepository: NotesRepository,
    private val encryptionRepository: EncryptionRepository,
    private val preferencesRepository: PreferencesRepository
) {
    suspend operator fun invoke(notebookPath: String, newName: String) {

        val isProtected = encryptionRepository.hasKey(notebookPath)

        println("DEBUG: RenameNotebookUseCase: old=$notebookPath new=$newName isProtected=$isProtected")

        if (isProtected) {
            notesRepository.decryptAllNotes(notebookPath)
            encryptionRepository.deleteKeyForNotebook(notebookPath)
        }

        notebookRepository.renameNotebook(notebookPath, newName)

        if (isProtected) {
            encryptionRepository.createKeyForNotebook(newName)
            notesRepository.encryptAllNotes(newName)
        }

        preferencesRepository.updateNotebookPathInRecent(notebookPath, newName)
        preferencesRepository.updateNotebookPreferences(notebookPath, newName)
    }
}