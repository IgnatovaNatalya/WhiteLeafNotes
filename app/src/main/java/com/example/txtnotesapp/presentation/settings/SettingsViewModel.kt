package com.example.txtnotesapp.presentation.settings

import android.net.Uri
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.use_case.ExportAllNotesUseCase
import com.example.txtnotesapp.domain.use_case.GetExportDirectoryUseCase
import com.example.txtnotesapp.domain.use_case.ImportZipNotesUseCase
import com.example.txtnotesapp.domain.use_case.SaveExportDirectoryUseCase
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getExportDirectoryUseCase: GetExportDirectoryUseCase,
    private val saveExportDirectoryUseCase: SaveExportDirectoryUseCase,
    private val exportNotesUseCase: ExportAllNotesUseCase,
    private val importNotesUseCase: ImportZipNotesUseCase
) : ViewModel() {

    private val _exportState = MutableLiveData<ExportState>()
    val exportState: LiveData<ExportState> = _exportState

    private val _directoryPath = MutableLiveData<String?>()
    val exportPath: LiveData<String?> = _directoryPath

    private val _importState = MutableLiveData<ImportState>()
    val importState: LiveData<ImportState> = _importState

    init {
//        viewModelScope.launch {
//            _directoryPath.value = getExportDirectoryUseCase()
//        }
        _directoryPath.postValue(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).name)
    }

    fun saveDirectory(path: String) {
        viewModelScope.launch {
            saveExportDirectoryUseCase(path)
            _directoryPath.value = path
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
                        ExportState.Error(result.exceptionOrNull()?.message ?: "Неизвестная ошиибка при экспорте")
                    )
                }
            } catch (e: Exception) {
                _exportState.postValue(ExportState.Error(e.message ?: "Ошибка экспорта"))
            }
        }
    }

    fun importNotesFromZip(zipFileUri: Uri) {
        viewModelScope.launch {
            _importState.postValue(ImportState.Loading)
            try {
                val result = importNotesUseCase.execute(zipFileUri)

                if (result.isSuccess)
                    _importState.postValue(ImportState.Success)
                else _importState.postValue(
                    ImportState.Error(result.exceptionOrNull()?.message ?: "Неизвестная ошибка при импорте")
                )
            } catch (e: Exception) {
                _importState.postValue(ImportState.Error(e.message ?: "Ошибка импорта"))
            }
        }
    }

}

