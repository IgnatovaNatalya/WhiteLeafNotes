package ru.whiteleaf.notes.presentation.note_edit

import android.net.Uri

sealed class NoteEditNavigationEvent {
    object NavigateBack : NoteEditNavigationEvent()
    data class ShareFile(val uri: Uri?) : NoteEditNavigationEvent()
    data class ShowMessage(val message: String) : NoteEditNavigationEvent()
    object ShowBiometric : NoteEditNavigationEvent()
//    data class ReopenNote(val newId:String): NoteEditNavigationEvent()
}