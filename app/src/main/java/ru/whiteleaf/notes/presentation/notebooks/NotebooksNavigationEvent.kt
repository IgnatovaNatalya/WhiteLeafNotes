package ru.whiteleaf.notes.presentation.notebooks

import android.net.Uri
import ru.whiteleaf.notes.domain.model.Notebook



sealed class NotebooksNavigationEvent {
    object Idle : NotebooksNavigationEvent()
    data class NavigateToCreatedNotebook(val notebook: Notebook) : NotebooksNavigationEvent()
    data class ShareUri(val uri: Uri?) : NotebooksNavigationEvent()
    data class ShowMessage(val msg: String?) : NotebooksNavigationEvent()
}