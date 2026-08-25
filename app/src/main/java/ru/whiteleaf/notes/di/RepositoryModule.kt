package ru.whiteleaf.notes.di

import org.koin.dsl.module
import ru.whiteleaf.notes.data.datasource.FileNoteDataSource
import ru.whiteleaf.notes.data.datasource.FileNotebookDataSource
import ru.whiteleaf.notes.data.repository.EncryptionRepositoryImpl
import ru.whiteleaf.notes.data.repository.ExportRepositoryImpl
import ru.whiteleaf.notes.data.repository.NoteRepositoryImpl
import ru.whiteleaf.notes.data.repository.NotebookRepositoryImpl
import ru.whiteleaf.notes.data.repository.PreferencesRepositoryImpl
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository

val repositoryModule = module {

    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource(get()) }

    // Repositories
    single<NotesRepository> { NoteRepositoryImpl(get(), get(), get()) }
    single<NotebookRepository> { NotebookRepositoryImpl(get(), get(), get()) }
    single<ExportRepository> { ExportRepositoryImpl(get(), get()) }
    single<EncryptionRepository> { EncryptionRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get(), get()) }
}