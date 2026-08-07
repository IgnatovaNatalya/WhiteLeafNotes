package ru.whiteleaf.notes.di


import android.content.ContentResolver
import android.content.Context
import com.google.gson.Gson
import ru.whiteleaf.notes.data.repository.NoteRepositoryImpl
import ru.whiteleaf.notes.data.repository.NotebookRepositoryImpl
import ru.whiteleaf.notes.data.repository.EncryptionRepositoryImpl
import ru.whiteleaf.notes.data.repository.ExportRepositoryImpl
import ru.whiteleaf.notes.data.datasource.FileNoteDataSource
import ru.whiteleaf.notes.data.datasource.FileNotebookDataSource
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.ExportRepository
import ru.whiteleaf.notes.domain.repository.NotesRepository
import ru.whiteleaf.notes.domain.repository.NotebookRepository
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.CreateNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportAllNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.share.GetSharedContentUseCase
import ru.whiteleaf.notes.domain.use_case.notes.InsertNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.share.ImportZipNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.SaveNoteContentUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNoteFileUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNotebookUseCase
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
import ru.whiteleaf.notes.data.repository.PreferencesRepositoryImpl
import ru.whiteleaf.notes.domain.interactor.SettingsInteractor
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import ru.whiteleaf.notes.domain.use_case.encryption.CountEncryptedNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.recent.GetRecentNotesUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookUnlockedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.LockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.recent.RemoveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.recent.SaveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateFullNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase
import java.security.KeyStore

val koinModule = module {

    // App
    single<ContentResolver> { androidContext().contentResolver }

    // Data sources
    single { FileNoteDataSource(get()) }
    single { FileNotebookDataSource(get()) }

    //prefs
    single {
        androidContext().getSharedPreferences(
            WHITE_LEAF_PREFS,
            Context.MODE_PRIVATE
        )
    }

    //keystore
    single {
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
        }
    }

    //Gson
    single<Gson> { Gson() }

    // Repositories
    single<NotesRepository> { NoteRepositoryImpl(get(), get(), get()) }
    single<NotebookRepository> { NotebookRepositoryImpl(get(), get(), get()) }
    single<ExportRepository> { ExportRepositoryImpl(get(), get()) }
    single<EncryptionRepository> { EncryptionRepositoryImpl(get()) }
    single<PreferencesRepository> { PreferencesRepositoryImpl(get(), get()) }

    // Use cases
    factory { GetNotesUseCase(get()) }
    factory { GetNoteUseCase(get()) }
    factory { CreateNoteUseCase(get()) }
    factory { RenameNoteUseCase(get(), get()) }
    factory { SaveNoteContentUseCase(get()) }
    factory { UpdateFullNoteUseCase(get(), get()) }
    factory { DeleteNoteUseCase(get(), get()) }
    factory { MoveNoteUseCase(get(), get()) }

    factory { ShareNoteFileUseCase(get()) }

    factory { GetNotebooksUseCase(get()) }
    factory { CreateNotebookUseCase(get()) }
    factory { RenameNotebookUseCase(get(), get(), get(), get()) }
    factory { DeleteNotebookByPathUseCase(get(), get()) }//, get(), get()) }

    factory { ShareNotebookUseCase(get(), get(), get()) }

    factory { ExportAllNotesUseCase(get(), get(), get(), get()) }
    factory { ImportZipNotesUseCase(get(), get(), get()) }

    factory { GetSharedContentUseCase(get()) }
    factory { InsertNoteUseCase(get()) }

    factory { UpdateNoteDateUseCase(get()) }

    factory { CreateKeyForNotebookUseCase(get()) }
    factory { DeleteKeyForNotebookUseCase(get()) }
    factory { IsNotebookProtectedUseCase(get()) }
    factory { IsNotebookUnlockedUseCase(get()) }
    factory { LockNotebookUseCase(get()) }
    factory { UnlockNotebookUseCase(get()) }

    factory { DecryptNotebookUseCase(get()) }
    factory { EncryptNotebookUseCase(get()) }
    factory { CountEncryptedNotebooksUseCase(get(), get()) }

    factory { GetRecentNotesUseCase(get()) }
    factory { SaveRecentNoteUseCase(get()) }
    factory { RemoveRecentNoteUseCase(get()) }

    //interactor
    factory { SettingsInteractor(get()) }

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
            unlockNotebookUseCase = get(),
            isNotebookProtectedUseCase = get(),
            getRecentNotesUseCase = get(),
        )
    }

    viewModel {
        DrawerMenuViewModel(
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

            preferencesInteractor = get(),

            isNotebookProtectedUseCase = get(),
            isNotebookUnlockedUseCase = get(),
            unlockNotebookUseCase = get(),
            lockNotebookUseCase = get(),
            createKeyForNotebookUseCase = get(),
            deleteKeyForNotebookUseCase = get(),
            encryptNotebookUseCase = get(),
            decryptNotebookUseCase = get(),

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
            updateFullNoteUseCase = get(),
            shareNoteFileUseCase = get(),
            updateNoteDateUseCase = get(),
            noteId = noteId,
            notebookPath = notebookPath,
            unlockNotebookUseCase = get(),
            settingsInteractor = get(),
            isNotebookProtectedUseCase = get(),
            saveRecentNoteUseCase = get(),
            removeRecentNoteUseCase = get(),
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

