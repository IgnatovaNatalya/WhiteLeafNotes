package ru.whiteleaf.notes.di

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
import ru.whiteleaf.notes.data.config.NotebookConfigManager
import ru.whiteleaf.notes.data.datasource.EncryptionManager
import ru.whiteleaf.notes.domain.use_case.DecryptExistingNotes
import ru.whiteleaf.notes.domain.use_case.ReEncryptExistingNotes

val koinModule = module {

    // App
    single<ContentResolver> { androidContext().contentResolver }

    //security
    single<EncryptionManager> { EncryptionManager() }
    single<NotebookConfigManager> {
        NotebookConfigManager(
            sharedPreferences = androidContext().getSharedPreferences(
                "txt_notes_prefs",
                Context.MODE_PRIVATE
            )
        )
    }

    // Data sources
    single {
        FileNoteDataSource(
            context = androidContext(),
            configManager = get(),
            encryptionManager = get()
        )
    }

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

    factory { ReEncryptExistingNotes(get()) }
    factory { DecryptExistingNotes( get()) }

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
            preferences = androidContext().getSharedPreferences(
                "txt_notes_prefs",
                Context.MODE_PRIVATE
            ),
            notebookPath = notebookPath,
            configManager = get(),
            encryptionManager = get(),
            reEncryptExistingNotes = get(),
            decryptExistingNotes = get(),
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

