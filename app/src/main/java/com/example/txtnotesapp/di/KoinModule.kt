package com.example.txtnotesapp.di

import android.content.Context
import com.example.txtnotesapp.data.NoteRepositoryImpl
import com.example.txtnotesapp.data.NotebookRepositoryImpl
import com.example.txtnotesapp.data.local.FileNoteDataSource
import com.example.txtnotesapp.data.local.FileNotebookDataSource
import com.example.txtnotesapp.domain.repository.NoteRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.CreateNotebook
import com.example.txtnotesapp.domain.use_case.DeleteNote
import com.example.txtnotesapp.domain.use_case.DeleteNotebook
import com.example.txtnotesapp.domain.use_case.GetNote
import com.example.txtnotesapp.domain.use_case.GetNotebooks
import com.example.txtnotesapp.domain.use_case.GetNotes
import com.example.txtnotesapp.domain.use_case.MoveNote
import com.example.txtnotesapp.domain.use_case.RenameNote
import com.example.txtnotesapp.domain.use_case.RenameNotebook
import com.example.txtnotesapp.domain.use_case.SaveNote
import com.example.txtnotesapp.domain.use_case.ShareNote
import com.example.txtnotesapp.presentation.note_edit.NoteEditViewModel
import com.example.txtnotesapp.presentation.note_list.NoteListViewModel
import com.example.txtnotesapp.presentation.notebooks.NotebooksViewModel
import com.example.txtnotesapp.presentation.start.StartViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {
    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource(get()) }

    // Repositories
    single<NoteRepository> { NoteRepositoryImpl(get(), get()) }
    single<NotebookRepository> { NotebookRepositoryImpl(get(), get(), get()) }

    // Use cases
    factory { GetNotes(get()) }
    factory { GetNote(get()) }
    factory { SaveNote(get()) }
    factory { CreateNote(get()) }
    factory { DeleteNote(get()) }
    factory { MoveNote(get()) }
    factory { RenameNote(get()) }
    factory { ShareNote(get()) }

    factory { GetNotebooks(get()) }
    factory { CreateNotebook(get()) }
    factory { DeleteNotebook(get()) }
    factory { RenameNotebook(get()) }

    // ViewModels

    viewModel {
        StartViewModel(
            getNotebooks = get(),
            getNotes = get(),
            createNote = get(),
            createNotebook = get()
        )
    }

    viewModel { (notebookPath: String?) ->
        NoteListViewModel(
            getNotes = get(),
            deleteNote = get(),
            createNote = get(),
            moveNote = get(),
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
}

