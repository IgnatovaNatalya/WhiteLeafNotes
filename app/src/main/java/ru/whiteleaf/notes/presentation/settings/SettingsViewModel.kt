package ru.whiteleaf.notes.presentation.settings

import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
import ru.whiteleaf.notes.domain.use_case.share.ExportAllNotesUseCase
import ru.whiteleaf.notes.domain.use_case.share.ImportZipNotesUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.use_case.encryption.CountEncryptedNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportProgressCallback

class SettingsViewModel(
    private val exportNotesUseCase: ExportAllNotesUseCase,
    private val importNotesUseCase: ImportZipNotesUseCase,
    private val countEncryptedNotebooksUseCase: CountEncryptedNotebooksUseCase

) : ViewModel() {


    private val _exportState = MutableLiveData<ExportState>()
    val exportState: LiveData<ExportState> = _exportState

    private val _importState = MutableLiveData<ImportState>()
    val importState: LiveData<ImportState> = _importState

    init {

        viewModelScope.launch {
            val path =
                "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).name} / $DEFAULT_DIR"
            val count = countEncryptedNotebooksUseCase()

            _exportState.postValue(ExportState.Idle(path, count))
        }
    }

    fun exportNotes(
        context: Context,
        shareFile: Boolean,
        exportEncrypted: Boolean,
        password: String? = null
    ) {
        viewModelScope.launch {
            _exportState.postValue(ExportState.Loading("Подготовка к экспорту"))
            try {
                val result = exportNotesUseCase(context, exportEncrypted, password,
                    progressCallback = object : ExportProgressCallback {
                        override fun onNotebookExportStarted(notebookName: String) {
                            _exportState.postValue(ExportState.Loading("Экспорт: $notebookName"))
                        }
                    })
                if (result.isSuccess) {
                    _exportState.postValue(ExportState.Success(if (shareFile) result.getOrNull() else null))
                } else {
                    _exportState.postValue(
                        ExportState.Error(
                            result.exceptionOrNull()?.message ?: "Неизвестная ошиибка при экспорте"
                        )
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
                    ImportState.Error(
                        result.exceptionOrNull()?.message ?: "Неизвестная ошибка при импорте"
                    )
                )
            } catch (e: Exception) {
                _importState.postValue(ImportState.Error(e.message ?: "Ошибка импорта"))
            }
        }
    }
}