package com.example.txtnotesapp.di

import android.content.Context
import com.example.txtnotesapp.data.repository.NoteRepositoryImpl
import com.example.txtnotesapp.data.repository.NotebookRepositoryImpl
import com.example.txtnotesapp.data.repository.PreferencesRepositoryImpl
import com.example.txtnotesapp.data.repository.ExternalRepositoryImpl
import com.example.txtnotesapp.data.datasource.FileNoteDataSource
import com.example.txtnotesapp.data.datasource.FileNotebookDataSource
import com.example.txtnotesapp.domain.repository.ExternalRepository
import com.example.txtnotesapp.domain.repository.NotesRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository
import com.example.txtnotesapp.domain.repository.PreferencesRepository
import com.example.txtnotesapp.domain.use_case.CreateNoteUseCase
import com.example.txtnotesapp.domain.use_case.CreateNotebookUseCase
import com.example.txtnotesapp.domain.use_case.DeleteNoteUseCase
import com.example.txtnotesapp.domain.use_case.DeleteNotebookUseCase
import com.example.txtnotesapp.domain.use_case.ExportAllNotesUseCase
import com.example.txtnotesapp.domain.use_case.GetNoteUseCase
import com.example.txtnotesapp.domain.use_case.GetNotebooksUseCase
import com.example.txtnotesapp.domain.use_case.GetNotesUseCase
import com.example.txtnotesapp.domain.use_case.GetExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.MoveNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNotebookUseCase
import com.example.txtnotesapp.domain.use_case.SaveNoteUseCase
import com.example.txtnotesapp.domain.use_case.SaveExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.ShareNoteFileUseCase
import com.example.txtnotesapp.presentation.note_edit.NoteEditViewModel
import com.example.txtnotesapp.presentation.note_list.NoteListViewModel
import com.example.txtnotesapp.presentation.notebooks.NotebooksViewModel
import com.example.txtnotesapp.presentation.root.DrawerMenuViewModel
import com.example.txtnotesapp.presentation.settings.SettingsViewModel
import com.example.txtnotesapp.presentation.shareReceive.ShareReceiverViewModel
import com.example.txtnotesapp.presentation.start.StartViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource(get()) }

    // Repositories
    single<NotesRepository> { NoteRepositoryImpl(get(), get()) }
    single<NotebookRepository> { NotebookRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }
    single<ExternalRepository> { ExternalRepositoryImpl(get(), get()) }

    // Use cases
    factory { GetNotesUseCase(get()) }
    factory { GetNoteUseCase(get()) }
    factory { SaveNoteUseCase(get()) }
    factory { CreateNoteUseCase(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { MoveNoteUseCase(get()) }
    factory { RenameNoteUseCase(get()) }
    factory { ShareNoteFileUseCase(get()) }

    factory { GetNotebooksUseCase(get()) }
    factory { CreateNotebookUseCase(get()) }
    factory { DeleteNotebookUseCase(get()) }
    factory { RenameNotebookUseCase(get()) }

    factory { GetExportDirectoryUseCase(get()) }
    factory { SaveExportDirectoryUseCase(get()) }
    factory { ExportAllNotesUseCase(get(), get(), get()) }

    // ViewModels

    viewModel {
        StartViewModel(
            getNotebooksUseCase = get(),
            getNotesUseCase = get(),
            createNoteUseCase = get(),
            createNotebookUseCase = get(),
            moveNoteUseCase = get(),
            renameNoteUseCase = get(),
            deleteNoteUseCase = get(),
            renameNotebookUseCase = get(),
            deleteNotebookUseCase = get(),
        )
    }

    viewModel {
        DrawerMenuViewModel(
            getNotebooksUseCase = get(),
            getNotesUseCase = get(),
            createNotebookUseCase = get(),
            createNoteUseCase = get()
        )
    }

    viewModel { (notebookPath: String?) ->
        NoteListViewModel(
            getNotesUseCase = get(),
            deleteNoteUseCase = get(),
            createNoteUseCase = get(),
            moveNoteUseCase = get(),
            renameNoteUseCase = get(),
            preferences = androidContext().getSharedPreferences(
                "txt_notes_prefs",
                Context.MODE_PRIVATE
            ),
            notebookPath = notebookPath,
        )
    }

    viewModel { (noteId: String?, notebookPath: String?) ->
        NoteEditViewModel(
            getNoteUseCase = get(),
            renameNoteUseCase = get(),
            moveNoteUseCase = get(),
            saveNoteUseCase = get(),
            shareNoteFileUseCase = get(),
            createNoteUseCase = get(),
            noteId = noteId,
            notebookPath = notebookPath
        )
    }

    viewModel {
        NotebooksViewModel(
            getNotebooks = get(),
            createNotebook = get(),
            deleteNotebook = get(),
            renameNotebook = get()
        )
    }

    viewModel {
        SettingsViewModel(
            getExportDirectoryUseCase = get(),
            saveExportDirectoryUseCase = get(),
            exportNotesUseCase = get()
        )
    }

    viewModel {
        ShareReceiverViewModel(
            createNoteUseCase = get(),
            renameNoteUseCase = get(),
            saveNoteUseCase = get(),
        )
    }
}

