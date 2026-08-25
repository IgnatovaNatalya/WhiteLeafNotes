package ru.whiteleaf.notes.di

import org.koin.dsl.module
import ru.whiteleaf.notes.domain.interactor.SettingsInteractor
import ru.whiteleaf.notes.domain.use_case.encryption.CountEncryptedNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookUnlockedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.LockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.CreateNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.PinNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.UnpinNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.InsertNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.SaveNoteContentUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateFullNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase
import ru.whiteleaf.notes.domain.use_case.recent.GetRecentNotesUseCase
import ru.whiteleaf.notes.domain.use_case.recent.RemoveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.recent.SaveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportAllNotesUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.share.GetSharedContentUseCase
import ru.whiteleaf.notes.domain.use_case.share.ImportZipNotesUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNoteFileUseCase

val domainModule = module {

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

    factory { ExportNotebookUseCase(get(), get(), get(), get()) }

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

    factory { PinNotebookUseCase(get()) }
    factory { UnpinNotebookUseCase(get()) }

    //interactor
    factory { SettingsInteractor(get()) }
}