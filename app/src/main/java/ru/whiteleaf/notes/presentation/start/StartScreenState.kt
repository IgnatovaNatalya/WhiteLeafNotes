package ru.whiteleaf.notes.presentation.start

import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook

sealed class StartScreenState {
    object Loading : StartScreenState()
    data class Success(val startScreenItems: List<StartListItem>) : StartScreenState()
    //data class Error(val message: String) : StartScreenState()
}

sealed class StartNavigationEvent {
    object Idle : StartNavigationEvent()
    data class NavigateToCreatedNote(val note: Note) : StartNavigationEvent()
    data class NavigateToCreatedNotebook(val notebook: Notebook) : StartNavigationEvent()
    data class ShareUri(val uri: Uri) : StartNavigationEvent()
}