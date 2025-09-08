package com.example.txtnotesapp.presentation.settings

import android.net.Uri

sealed class ExportState {
    object Idle : ExportState()
    object Loading : ExportState()
    data class Success(val fileUri: Uri?) : ExportState()
    data class Error(val message: String) : ExportState()
}