package com.example.whiteleafnotes.presentation.root

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.use_case.CreateNoteUseCase
import com.example.whiteleafnotes.domain.use_case.CreateNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.GetNotebooksUseCase
import com.example.whiteleafnotes.domain.use_case.GetNotesUseCase
import kotlinx.coroutines.launch

class DrawerMenuViewModel(
    private val getNotebooksUseCase: GetNotebooksUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val createNotebookUseCase: CreateNotebookUseCase,
    private val createNoteUseCase: CreateNoteUseCase
) : ViewModel() {

    private val _menuItems = MutableLiveData<List<DrawerMenuItem>>()
    val menuItems: LiveData<List<DrawerMenuItem>> = _menuItems

    private val _navigateToCreatedNote = MutableLiveData<Note?>()
    val navigateToCreatedNote: LiveData<Note?> = _navigateToCreatedNote

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    init {
        loadMenuData()
    }

    fun loadMenuData() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val notebooks = getNotebooksUseCase()
                val rootNotes = getNotesUseCase(null)

                val items = buildMenuItems(notebooks, rootNotes)
                _menuItems.value = items
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки данных меню: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildMenuItems(notebooks: List<Notebook>, rootNotes: List<Note>): List<DrawerMenuItem> {
        val items = mutableListOf<DrawerMenuItem>()

        // Заголовок для записных книжек
        //items.add(DrawerMenuItem.Header("ЗАПИСНЫЕ КНИЖКИ"))

        // Элементы записных книжек
        if (notebooks.isEmpty()) {
            //items.add(DrawerMenuItem.EmptyNotebooks)
        } else {
            notebooks.forEach { notebook ->
                items.add(DrawerMenuItem.NotebookItem(notebook))
            }
        }

        items.add(DrawerMenuItem.CreateNotebook)
        items.add(DrawerMenuItem.Divider)

        // Заголовок для заметок
        //items.add(DrawerMenuItem.Header("ЗАМЕТКИ"))

        if (rootNotes.isEmpty()) {
            //items.add(DrawerMenuItem.EmptyNotes)
        } else {
            rootNotes.forEach { note ->
                items.add(DrawerMenuItem.NoteItem(note))
                items.add(DrawerMenuItem.Divider)
            }
        }
        items.add(DrawerMenuItem.CreateNote)
        return items
    }

    fun createNewNotebook(name: String) {
        viewModelScope.launch {
            try {
                createNotebookUseCase(name)
                loadMenuData() // Перезагружаем данные после создания
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

    fun clearError() {
        _error.value = null
    }
}