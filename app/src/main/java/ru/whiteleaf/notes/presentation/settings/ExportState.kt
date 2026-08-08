package ru.whiteleaf.notes.presentation.settings

import android.net.Uri

sealed class ExportState {
    data class Idle(val path: String, val numberEncrypted: Int) : ExportState()
    data class Loading(val message: String) : ExportState()
    data class Success(val fileUri: Uri?) : ExportState()
    data class Error(val message: String) : ExportState()
}
