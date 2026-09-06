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

class RootViewModel(
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

    private val _searchQuery = MutableLiveData<String?>()
    val searchQuery: LiveData<String?> = _searchQuery

    private val _isSearchExpanded = MutableLiveData<Boolean>()
    val isSearchExpanded: LiveData<Boolean> = _isSearchExpanded


    init {
        viewModelScope.launch { clearScrollPositionsUseCase() }
    }

    fun isSearching():Boolean {
//        val query = _searchQuery.value?: return false
//        return query.length>=3
        println("DEBUG: RootVM: isSearchExpanded=${isSearchExpanded.value}, searchQuery=${searchQuery.value} ")
        return isSearchExpanded.value == true && searchQuery.value?.isNotEmpty()?:false
    }

    fun setSearchQuery(query: String?) {
        _searchQuery.value = query
    }

    fun setSearchExpanded(expanded: Boolean) {
        _isSearchExpanded.value = expanded
    }

    fun clearSearch() {
        println("DEBUG: RootVM: search cleared")
        _searchQuery.value = null
        _isSearchExpanded.value = false
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