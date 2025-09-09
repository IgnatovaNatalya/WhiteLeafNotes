package com.example.txtnotesapp.presentation.notebooks

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.use_case.CreateNotebookUseCase
import com.example.txtnotesapp.domain.use_case.DeleteNotebookUseCase
import com.example.txtnotesapp.domain.use_case.GetNotebooksUseCase
import com.example.txtnotesapp.domain.use_case.RenameNotebookUseCase
import kotlinx.coroutines.launch

class NotebooksViewModel(
    private val getNotebooks: GetNotebooksUseCase,
    private val createNotebook: CreateNotebookUseCase,
    private val deleteNotebook: DeleteNotebookUseCase,
    private val renameNotebook: RenameNotebookUseCase
) : ViewModel() {

    private val _notebooks = MutableLiveData<List<Notebook>>()
    val notebooks: LiveData<List<Notebook>> = _notebooks

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadNotebooks()
    }

    fun loadNotebooks() {
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val notebooksList = getNotebooks()
                _notebooks.value = notebooksList
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки записных книжек: ${e.message}"
                _notebooks.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewNotebook(name: String) {
        viewModelScope.launch {
            try {
                createNotebook(name)
                // Перезагружаем список после создания
                loadNotebooks()
            } catch (e: Exception) {
                _error.value = "Ошибка создания записной книжки: ${e.message}"
            }
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            try {
                deleteNotebook(notebook)
                // Перезагружаем список после удаления
                loadNotebooks()
            } catch (e: Exception) {
                _error.value = "Ошибка удаления записной книжки: ${e.message}"
            }
        }
    }

    fun renameNotebook(notebook: Notebook, newName: String) {
        viewModelScope.launch {
            try {
                renameNotebook(notebook, newName)
                // Перезагружаем список после переименования
                loadNotebooks()
            } catch (e: Exception) {
                _error.value = "Ошибка переименования записной книжки: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}