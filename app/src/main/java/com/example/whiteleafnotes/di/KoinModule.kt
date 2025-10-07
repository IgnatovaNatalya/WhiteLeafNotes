package com.example.whiteleafnotes.di

import android.content.ContentResolver
import android.content.Context
import com.example.whiteleafnotes.data.repository.NoteRepositoryImpl
import com.example.whiteleafnotes.data.repository.NotebookRepositoryImpl
import com.example.whiteleafnotes.data.repository.ExportRepositoryImpl
import com.example.whiteleafnotes.data.datasource.FileNoteDataSource
import com.example.whiteleafnotes.data.datasource.FileNotebookDataSource
import com.example.whiteleafnotes.domain.repository.ExportRepository
import com.example.whiteleafnotes.domain.repository.NotesRepository
import com.example.whiteleafnotes.domain.repository.NotebookRepository
import com.example.whiteleafnotes.domain.use_case.CreateNoteUseCase
import com.example.whiteleafnotes.domain.use_case.CreateNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.DeleteNoteUseCase
import com.example.whiteleafnotes.domain.use_case.DeleteNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.ExportAllNotesUseCase
import com.example.whiteleafnotes.domain.use_case.GetNoteUseCase
import com.example.whiteleafnotes.domain.use_case.GetNotebooksUseCase
import com.example.whiteleafnotes.domain.use_case.GetNotesUseCase
import com.example.whiteleafnotes.domain.use_case.GetSharedContentUseCase
import com.example.whiteleafnotes.domain.use_case.InsertNoteUseCase
import com.example.whiteleafnotes.domain.use_case.MoveNoteUseCase
import com.example.whiteleafnotes.domain.use_case.RenameNoteUseCase
import com.example.whiteleafnotes.domain.use_case.RenameNotebookByPathUseCase
import com.example.whiteleafnotes.domain.use_case.DeleteNotebookByPathUseCase
import com.example.whiteleafnotes.domain.use_case.ImportZipNotesUseCase
import com.example.whiteleafnotes.domain.use_case.RenameNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.SaveNoteUseCase
import com.example.whiteleafnotes.domain.use_case.ShareNoteFileUseCase
import com.example.whiteleafnotes.domain.use_case.ShareNotebookUseCase
import com.example.whiteleafnotes.presentation.note_edit.NoteEditViewModel
import com.example.whiteleafnotes.presentation.note_list.NoteListViewModel
import com.example.whiteleafnotes.presentation.root.DrawerMenuViewModel
import com.example.whiteleafnotes.presentation.settings.SettingsViewModel
import com.example.whiteleafnotes.presentation.shareReceive.ShareReceiverViewModel
import com.example.whiteleafnotes.presentation.start.StartViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {

    // App
    single<ContentResolver> { androidContext().contentResolver }

    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource(get()) }

    // Repositories
    single<NotesRepository> { NoteRepositoryImpl(get(), get()) }
    single<NotebookRepository> { NotebookRepositoryImpl(get()) }
    single<ExportRepository> { ExportRepositoryImpl(get(), get()) }

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
    factory { RenameNotebookByPathUseCase(get()) }
    factory { DeleteNotebookByPathUseCase(get()) }
    factory { ShareNotebookUseCase(get(), get(), get()) }

    factory { ExportAllNotesUseCase(get(), get(), get()) }
    factory { ImportZipNotesUseCase(get(), get(), get()) }

    factory { GetSharedContentUseCase(get()) }
    factory { InsertNoteUseCase(get()) }

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
            renameNotebookUseCase = get(),
            deleteNotebookUseCase = get(),
            shareNotebookUseCase = get(),
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
            deleteNoteUseCase = get(),
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
        SettingsViewModel(
            exportNotesUseCase = get(),
            importNotesUseCase = get()
        )
    }

    viewModel {
        ShareReceiverViewModel(
            getSharedContent = get(),
            insertNoteUseCase = get(),
        )
    }
}

