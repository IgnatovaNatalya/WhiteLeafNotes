package com.example.txtnotesapp.di

import android.content.Context
import com.example.txtnotesapp.data.repository.NoteRepositoryImpl
import com.example.txtnotesapp.data.repository.NotebookRepositoryImpl
import com.example.txtnotesapp.data.repository.PreferencesRepositoryImpl
import com.example.txtnotesapp.data.repository.ExternalDataSourceImpl
import com.example.txtnotesapp.data.datasource.FileNoteDataSource
import com.example.txtnotesapp.data.datasource.FileNotebookDataSource
import com.example.txtnotesapp.domain.repository.ExternalRepository
import com.example.txtnotesapp.domain.repository.NotesRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository
import com.example.txtnotesapp.domain.repository.PreferencesRepository
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.CreateNotebook
import com.example.txtnotesapp.domain.use_case.DeleteNoteUseCase
import com.example.txtnotesapp.domain.use_case.DeleteNotebook
import com.example.txtnotesapp.domain.use_case.ExportNotesUseCase
import com.example.txtnotesapp.domain.use_case.GetNote
import com.example.txtnotesapp.domain.use_case.GetNotebooks
import com.example.txtnotesapp.domain.use_case.GetNotes
import com.example.txtnotesapp.domain.use_case.GetExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.MoveNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNotebook
import com.example.txtnotesapp.domain.use_case.SaveNote
import com.example.txtnotesapp.domain.use_case.SaveExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.ShareNote
import com.example.txtnotesapp.presentation.note_edit.NoteEditViewModel
import com.example.txtnotesapp.presentation.note_list.NoteListViewModel
import com.example.txtnotesapp.presentation.notebooks.NotebooksViewModel
import com.example.txtnotesapp.presentation.root.DrawerMenuViewModel
import com.example.txtnotesapp.presentation.settings.SettingsViewModel
import com.example.txtnotesapp.presentation.start.StartViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource(get()) }
    single<ExternalRepository> { ExternalDataSourceImpl(get(), get()) }

    // Repositories
    single<NotesRepository> { NoteRepositoryImpl(get(), get(), get()) }
    single<NotebookRepository> { NotebookRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get()) }

    // Use cases
    factory { GetNotes(get()) }
    factory { GetNote(get()) }
    factory { SaveNote(get()) }
    factory { CreateNote(get()) }
    factory { DeleteNoteUseCase(get()) }
    factory { MoveNoteUseCase(get()) }
    factory { RenameNoteUseCase(get()) }
    factory { ShareNote(get()) }

    factory { GetNotebooks(get()) }
    factory { CreateNotebook(get()) }
    factory { DeleteNotebook(get()) }
    factory { RenameNotebook(get()) }

    factory { GetExportDirectoryUseCase(get()) }
    factory { SaveExportDirectoryUseCase(get()) }
    factory { ExportNotesUseCase(get(), get()) }

    // ViewModels

    viewModel {
        StartViewModel(
            getNotebooks = get(),
            getNotes = get(),
            createNote = get(),
            createNotebook = get(),
            moveNoteUseCase = get(),
            renameNoteUseCase = get(),
            deleteNoteUseCase = get(),
        )
    }

    viewModel {
        DrawerMenuViewModel(
            getNotebooks = get(),
            getNotes = get(),
            createNotebook = get(),
            createNote = get()
        )
    }

    viewModel { (notebookPath: String?) ->
        NoteListViewModel(
            getNotes = get(),
            deleteNoteUseCase = get(),
            createNote = get(),
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
            getNote = get(),
            saveNote = get(),
            createNote = get(),
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
}

