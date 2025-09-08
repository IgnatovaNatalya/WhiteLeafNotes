package com.example.txtnotesapp.presentation.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.use_case.GetExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.SaveExportDirectoryUseCase
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getNotesDirectoryUseCase: GetExportDirectoryUseCase,
    private val saveNotesDirectoryUseCase: SaveExportDirectoryUseCase
) : ViewModel() {

    private val _currentPath = MutableLiveData<String?>(null)
    val currentPath: LiveData<String?> = _currentPath

    init {
        viewModelScope.launch {
            _currentPath.value = getNotesDirectoryUseCase()
        }
    }

    fun saveCustomDirectory(path: String) {
        viewModelScope.launch {
            saveNotesDirectoryUseCase(path)
            _currentPath.value = path
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            saveNotesDirectoryUseCase("")
            _currentPath.value = null
        }
    }
}