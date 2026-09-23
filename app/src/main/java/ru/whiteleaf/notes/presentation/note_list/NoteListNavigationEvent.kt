package ru.whiteleaf.notes.presentation.note_list

import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.presentation.note_list.UnlockTarget.ToCreate
import ru.whiteleaf.notes.presentation.note_list.UnlockTarget.ToLoad
import ru.whiteleaf.notes.presentation.note_list.UnlockTarget.ToSearch

sealed class NoteListNavigationEvent {
    object Idle : NoteListNavigationEvent()
    data class NavigateToNote(val noteId: String) : NoteListNavigationEvent()
    object NavigateBack : NoteListNavigationEvent()
    data class ReopenNotebook(val path: String) : NoteListNavigationEvent()
    data class ExportLink(val uri: Uri?) : NoteListNavigationEvent()
    data class ShareNote(val note: Note) : NoteListNavigationEvent()
    data class ShowBiometric(val unlockTarget: UnlockTarget) : NoteListNavigationEvent()
    data class ShowMessage(val msg:String) : NoteListNavigationEvent()
    data class NavigateToNoteFound(val noteId: String, val contentPosition: Int, val query: String?) :
        NoteListNavigationEvent()
}

sealed class UnlockTarget {
    object ToCreate : UnlockTarget()
    object ToSearch : UnlockTarget()
    object ToLoad : UnlockTarget()
}

fun UnlockTarget.toMessage(): String {
    return when (this) {
        ToCreate -> "Для создания"
        ToLoad -> "Для просмотра"
        is ToSearch -> "Для поиска"
    }
}
