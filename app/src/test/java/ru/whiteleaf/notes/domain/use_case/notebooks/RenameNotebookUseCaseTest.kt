package ru.whiteleaf.notes.domain.use_case.notebooks

import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.mockito.ArgumentMatchers.any
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import kotlinx.coroutines.test.runTest

@RunWith(MockitoJUnitRunner::class)
class RenameNotebookUseCaseTest {

    @Mock
    private lateinit var notebookRepository: NotebookRepository

    @Mock
    private lateinit var notesRepository: NotesRepository

    @Mock
    private lateinit var encryptionRepository: EncryptionRepository

    @Mock
    private lateinit var preferencesRepository: PreferencesRepository

    private lateinit var renameNotebookUseCase: RenameNotebookUseCase

    @Before
    fun setup() {
        renameNotebookUseCase = RenameNotebookUseCase(
            notebookRepository,
            notesRepository,
            encryptionRepository,
            preferencesRepository
        )
    }

    @Test
    fun `RenameNotebookUseCase should call repository renameNotebook`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(false)

        renameNotebookUseCase(oldPath, newName)

        verify(notebookRepository).renameNotebook(oldPath, newName)
    }

    @Test
    fun `RenameNotebookUseCase should check if notebook is protected`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(encryptionRepository).hasKey(oldPath)
    }

    @Test
    fun `RenameNotebookUseCase should decrypt notes when notebook is protected`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(notesRepository).decryptAllNotes(oldPath)
    }

    @Test
    fun `RenameNotebookUseCase should delete encryption key when decrypting`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(encryptionRepository).deleteKeyForNotebook(oldPath)
    }

    @Test
    fun `RenameNotebookUseCase should create new encryption key when re-encrypting`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(encryptionRepository).createKeyForNotebook(newName)
    }

    @Test
    fun `RenameNotebookUseCase should re-encrypt notes with new name`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(notesRepository).encryptAllNotes(newName)
    }

    @Test
    fun `RenameNotebookUseCase should call preferences updateNotebookPathInRecent`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(preferencesRepository).updateNotebookPathInRecent(oldPath, newName)
    }

    @Test
    fun `RenameNotebookUseCase should call preferences updateNotebookPreferences`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(true)

        renameNotebookUseCase(oldPath, newName)

        verify(preferencesRepository).updateNotebookPreferences(oldPath, newName)
    }

    @Test
    fun `RenameNotebookUseCase should not decrypt when notebook is not protected`() = runTest {
        val oldPath = "Old Notebook"
        val newName = "New Notebook"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(false)

        renameNotebookUseCase(oldPath, newName)

        verify(notesRepository, never()).decryptAllNotes(any())
        verify(encryptionRepository, never()).deleteKeyForNotebook(any())
        verify(encryptionRepository, never()).createKeyForNotebook(any())
        verify(notesRepository, never()).encryptAllNotes(any())
    }

    @Test
    fun `RenameNotebookUseCase should handle russian notebook name`() = runTest {
        val oldPath = "old"
        val newName = "Рабочие заметки"

        whenever(encryptionRepository.hasKey(oldPath)).thenReturn(false)

        renameNotebookUseCase(oldPath, newName)

        verify(notebookRepository).renameNotebook(oldPath, "Рабочие заметки")
    }
}
