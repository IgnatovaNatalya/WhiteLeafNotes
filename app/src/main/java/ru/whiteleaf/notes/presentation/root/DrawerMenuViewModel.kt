package ru.whiteleaf.notes.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.CreateNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.use_case.scroll.ClearScrollPositionsUseCase

class DrawerMenuViewModel(
    private val createNotebookUseCase: CreateNotebookUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val clearScrollPositionsUseCase: ClearScrollPositionsUseCase
) : ViewModel() {

    private val _navigateToCreatedNote = MutableLiveData<Note?>()
    val navigateToCreatedNote: LiveData<Note?> = _navigateToCreatedNote

    private val _navigateToCreatedNotebook = MutableLiveData<Notebook?>()
    val navigateToCreatedNotebook: LiveData<Notebook?> = _navigateToCreatedNotebook

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        viewModelScope.launch { clearScrollPositionsUseCase() }
    }

    fun createNewNotebook(name: String) {
        viewModelScope.launch {
            try {
                val newNotebook = createNotebookUseCase(name)
                _navigateToCreatedNotebook.value = newNotebook
            } catch (e: Exception) {
                _error.value = "Ошибка создания записной книжки: ${e.message}"
            }
        }
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(null)
                _navigateToCreatedNote.value = newNote
            } catch (e: Exception) {
                _error.value = "Ошибка создания заметки: ${e.message}"
            }
        }
    }

    fun onNoteNavigated() {
        _navigateToCreatedNote.value = null
    }

    fun onNotebookNavigated() {
        _navigateToCreatedNotebook.value = null
    }

    fun clearError() {
        _error.value = null
    }
}