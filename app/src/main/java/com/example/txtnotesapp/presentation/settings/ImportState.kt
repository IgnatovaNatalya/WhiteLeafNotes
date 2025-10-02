package com.example.txtnotesapp.presentation.settings

sealed class ImportState {
    object Success : ImportState()
    data class Error(val message: String) : ImportState()
    object Loading : ImportState()
    object Idle : ImportState()
}