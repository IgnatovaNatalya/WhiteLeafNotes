package ru.whiteleaf.notes.di

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.whiteleaf.notes.presentation.note_edit.NoteEditViewModel
import ru.whiteleaf.notes.presentation.note_list.NoteListViewModel
import ru.whiteleaf.notes.presentation.notebooks.NotebooksViewModel
import ru.whiteleaf.notes.presentation.root.DrawerMenuViewModel
import ru.whiteleaf.notes.presentation.settings.SettingsViewModel
import ru.whiteleaf.notes.presentation.shareReceive.ShareReceiverViewModel
import ru.whiteleaf.notes.presentation.start.StartViewModel


val viewmodelModule = module {

    viewModel {
        StartViewModel(
            getNotebooksUseCase = get(),
            getNotesUseCase = get(),
            createNoteUseCase = get(),
            createNotebookUseCase = get(),
            moveNoteUseCase = get(),
            renameNoteUseCase = get(),
            updateNoteDateUseCase = get(),
            deleteNoteUseCase = get(),
            renameNotebookUseCase = get(),
            deleteNotebookUseCase = get(),
            exportNotebookUseCase = get(),
            unlockNotebookUseCase = get(),
            isNotebookProtectedUseCase = get(),
            getRecentNotesUseCase = get(),
            createKeyForNotebookUseCase = get(),
            deleteKeyForNotebookUseCase = get(),
            encryptNotebookUseCase = get(),
            decryptNotebookUseCase = get(),
            pinNotebookUseCase = get(),
            unpinNotebookUseCase = get(),
        )
    }

    viewModel {
        DrawerMenuViewModel(
            createNotebookUseCase = get(),
            createNoteUseCase = get()
        )
    }

    viewModel {
        NotebooksViewModel(
            getNotebooksUseCase = get(),
            createNotebookUseCase = get(),
            renameNotebookUseCase = get(),
            deleteNotebookUseCase = get(),
            exportNotebookUseCase = get(),
            unlockNotebookUseCase = get(),
            isNotebookProtectedUseCase = get(),
            createKeyForNotebookUseCase = get(),
            deleteKeyForNotebookUseCase = get(),
            encryptNotebookUseCase = get(),
            decryptNotebookUseCase = get(),
            pinNotebookUseCase = get(),
            unpinNotebookUseCase = get()
        )
    }

    viewModel { (notebookPath: String?) ->
        NoteListViewModel(
            getNotesUseCase = get(),
            deleteNoteUseCase = get(),
            createNoteUseCase = get(),
            moveNoteUseCase = get(),
            updateNoteDateUseCase = get(),
            renameNoteUseCase = get(),

            renameNotebookUseCase = get(),
            deleteNotebookUseCase = get(),
            exportNotebookUseCase = get(),

            preferencesInteractor = get(),

            isNotebookProtectedUseCase = get(),
            isNotebookUnlockedUseCase = get(),
            unlockNotebookUseCase = get(),
            lockNotebookUseCase = get(),
            createKeyForNotebookUseCase = get(),
            deleteKeyForNotebookUseCase = get(),
            encryptNotebookUseCase = get(),
            decryptNotebookUseCase = get(),
            getNotebooksUseCase = get(),
            notebookPath = notebookPath,
        )
    }

    viewModel { (noteId: String?, notebookPath: String?) ->
        NoteEditViewModel(
            getNoteUseCase = get(),
            deleteNoteUseCase = get(),
            renameNoteUseCase = get(),
            moveNoteUseCase = get(),
            saveNoteContentUseCase = get(),
            shareNoteFileUseCase = get(),
            updateNoteDateUseCase = get(),
            noteId = noteId,
            notebookPath = notebookPath,
            unlockNotebookUseCase = get(),
            settingsInteractor = get(),
            isNotebookProtectedUseCase = get(),
            saveRecentNoteUseCase = get(),
            removeRecentNoteUseCase = get(),
            getNotebooksUseCase = get(),
        )
    }

    viewModel {
        SettingsViewModel(
            exportNotesUseCase = get(),
            importNotesUseCase = get(),
            countEncryptedNotebooksUseCase = get(),
        )
    }

    viewModel {
        ShareReceiverViewModel(
            getSharedContent = get(),
            insertNoteUseCase = get(),
        )
    }
}