package com.example.txtnotesapp.presentation.settings

import android.provider.MediaStore
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.use_case.ExportNotesUseCase
import com.example.txtnotesapp.domain.use_case.GetExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.SaveExportDirectoryUseCase
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getExportDirectoryUseCase: GetExportDirectoryUseCase,
    private val saveExportDirectoryUseCase: SaveExportDirectoryUseCase,
    private val exportNotesUseCase: ExportNotesUseCase
) : ViewModel() {

    private val _exportState = MutableLiveData<ExportState>()
    val exportState: LiveData<ExportState> = _exportState

    private val _exportPath = MutableLiveData<String?>()
    val exportPath: LiveData<String?> = _exportPath

    init {
//        viewModelScope.launch {
//            _exportPath.value = getExportDirectoryUseCase()
//        }
        _exportPath.postValue(MediaStore.Downloads.DISPLAY_NAME)
    }

    fun saveExportDirectory(path: String) {
        viewModelScope.launch {
            saveExportDirectoryUseCase(path)
            _exportPath.value = path
        }
    }

    fun exportNotes(password: String? = null) {
        viewModelScope.launch {
            _exportState.postValue(ExportState.Loading)
            try {
                val result = exportNotesUseCase(password)
                if (result.isSuccess) {
                    _exportState.postValue(ExportState.Success(result.getOrNull()))
                } else {
                    _exportState.postValue(
                        ExportState.Error(result.exceptionOrNull()?.message ?: "Unknown error"))
                }
            } catch (e: Exception) {
                _exportState.postValue(ExportState.Error(e.message ?: "Export failed"))
            }
        }
    }

    fun resetState() {
        _exportState.value = ExportState.Idle
    }
}

