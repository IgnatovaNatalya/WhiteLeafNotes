package ru.whiteleaf.notes.presentation.note_edit

import android.net.Uri
import ru.whiteleaf.notes.domain.model.Note

sealed class NoteEditNavigationEvent {
    object NavigateBack : NoteEditNavigationEvent()
    data class ShareFile(val uri: Uri?) : NoteEditNavigationEvent()
    data class ShareNote(val note: Note) : NoteEditNavigationEvent()
    data class ShowMessage(val message: String) : NoteEditNavigationEvent()
    object ShowBiometric : NoteEditNavigationEvent()
}