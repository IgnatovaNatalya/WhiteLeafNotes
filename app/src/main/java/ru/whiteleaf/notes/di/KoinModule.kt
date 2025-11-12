package ru.whiteleaf.notes.di

import BiometricRepositoryImpl
import SecurityPreferencesImpl
import android.content.ContentResolver
import android.content.Context
import ru.whiteleaf.notes.data.repository.NoteRepositoryImpl
import ru.whiteleaf.notes.data.repository.NotebookRepositoryImpl
import ru.whiteleaf.notes.data.repository.ExportRepositoryImpl
import ru.whiteleaf.notes.data.datasource.FileNoteDataSource
import ru.whiteleaf.notes.data.datasource.FileNotebookDataSource
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.use_case.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.CreateNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.ExportAllNotesUseCase
import ru.whiteleaf.notes.domain.use_case.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.GetSharedContentUseCase
import ru.whiteleaf.notes.domain.use_case.InsertNoteUseCase
import ru.whiteleaf.notes.domain.use_case.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.ImportZipNotesUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.SaveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.ShareNoteFileUseCase
import ru.whiteleaf.notes.domain.use_case.ShareNotebookUseCase
import ru.whiteleaf.notes.presentation.note_edit.NoteEditViewModel
import ru.whiteleaf.notes.presentation.note_list.NoteListViewModel
import ru.whiteleaf.notes.presentation.root.DrawerMenuViewModel
import ru.whiteleaf.notes.presentation.settings.SettingsViewModel
import ru.whiteleaf.notes.presentation.shareReceive.ShareReceiverViewModel
import ru.whiteleaf.notes.presentation.start.StartViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import ru.whiteleaf.notes.common.AppConstants.WHITE_LEAF_PREFS
import ru.whiteleaf.notes.data.repository.EncryptionRepositoryImpl
import ru.whiteleaf.notes.domain.repository.BiometricRepository
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.SecurityPreferences
import ru.whiteleaf.notes.domain.use_case.CheckNotebookAccessUseCase
import ru.whiteleaf.notes.domain.use_case.ClearNotebookKeysUseCase
import ru.whiteleaf.notes.domain.use_case.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.LockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.UnlockNotebookUseCase

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

    single<BiometricRepository> { BiometricRepositoryImpl(get()) }
    single<EncryptionRepository> { EncryptionRepositoryImpl(get(), get(), get()) }
    single<SecurityPreferences> { SecurityPreferencesImpl(androidContext().getSharedPreferences(
        WHITE_LEAF_PREFS,
        Context.MODE_PRIVATE)) }

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
    factory { DeleteNotebookByPathUseCase(get(), get(), get()) }
    factory { ShareNotebookUseCase(get(), get(), get()) }

    factory { ExportAllNotesUseCase(get(), get(), get()) }
    factory { ImportZipNotesUseCase(get(), get(), get()) }

    factory { GetSharedContentUseCase(get()) }
    factory { InsertNoteUseCase(get()) }

    factory { EncryptNotebookUseCase(get(), get()) }
    factory { UnlockNotebookUseCase(get(), get(), get()) }
    factory { CheckNotebookAccessUseCase(get(), get()) }
    factory { LockNotebookUseCase(get(), get()) }

    factory { ClearNotebookKeysUseCase(get()) }

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
            shareNotebookUseCase = get(),
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

            encryptNotebookUseCase = get(),
            unlockNotebookUseCase = get(),
            checkNotebookAccessUseCase = get(),
            lockNotebookUseCase = get(),

            preferences = androidContext().getSharedPreferences(
                WHITE_LEAF_PREFS,
                Context.MODE_PRIVATE
            ),
            notebookPath = notebookPath,
            securityPreferences = get(),
            clearNotebookKeys = get(),
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
            encryptionRepository = get(),
            securityPreferences = get(),
            noteId = noteId,
            notebookPath = notebookPath,
            checkNotebookAccessUseCase = get()
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

