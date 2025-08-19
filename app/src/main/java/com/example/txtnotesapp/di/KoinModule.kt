package com.example.txtnotesapp.di

import android.content.SharedPreferences
import com.example.txtnotesapp.data.NoteRepositoryImpl
import com.example.txtnotesapp.data.NotebookRepositoryImpl
import com.example.txtnotesapp.data.local.FileNoteDataSource
import com.example.txtnotesapp.data.local.FileNotebookDataSource
import com.example.txtnotesapp.domain.repository.NoteRepository
import com.example.txtnotesapp.domain.repository.NotebookRepository
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.GetNotes
import com.example.txtnotesapp.presentation.note_edit.NoteEditViewModel
import com.example.txtnotesapp.presentation.note_list.NoteListViewModel
import com.example.txtnotesapp.presentation.notebooks.NotebooksViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val koinModule = module {

    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource() }

    // Repositories
    single<NoteRepository> { NoteRepositoryImpl(get()) }
    single<NotebookRepository> { NotebookRepositoryImpl() }

    // Use cases
    factory { GetNotes() }
    factory { CreateNote() }
    factory { (notebookPath: String) -> GetNotes() }
    //  todo другие use cases

    // ViewModels
    viewModel { (notebookPath: String?) ->
        NoteListViewModel(get(), get(), notebookPath)
    }
    viewModel { (noteId: String) -> NoteEditViewModel() }
    viewModel { NotebooksViewModel() }
    viewModel { (notebookPath: String) -> NoteListViewModel(get(), get(), notebookPath) }
}
