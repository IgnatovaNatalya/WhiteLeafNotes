package com.example.txtnotesapp.presentation.start

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.CreateNotebook
import com.example.txtnotesapp.domain.use_case.GetNotebooks
import com.example.txtnotesapp.domain.use_case.GetNotes
import kotlinx.coroutines.launch
import kotlin.collections.forEach

class StartViewModel(
    private val getNotebooks: GetNotebooks,
    private val getNotes: GetNotes,
    private val createNote: CreateNote,
    private val createNotebook: CreateNotebook
) : ViewModel() {

    private val _startItems = MutableLiveData<List<StartListItem>>()
    val startItems: LiveData<List<StartListItem>> = _startItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    init {
        loadData()
    }

    fun loadData() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val notebooks = getNotebooks()
                val rootNotes = getNotes(null) // Заметки в корневой папке

                val items = buildStartItems(notebooks, rootNotes)
                _startItems.value = items
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildStartItems(notebooks: List<Notebook>, rootNotes: List<Note>): List<StartListItem> {
        val items = mutableListOf<StartListItem>()

        // Секция записных книжек
        items.add(StartListItem.Header("ЗАПИСНЫЕ КНИЖКИ"))

        if (notebooks.isEmpty()) {
            items.add(StartListItem.EmptyNotebooks)
        } else {
            notebooks.forEach { notebook ->
                items.add(StartListItem.NotebookItem(notebook))
            }
        }

        items.add(StartListItem.AddNotebookButton)
        items.add(StartListItem.Divider)

        // Секция заметок
        items.add(StartListItem.Header("ЗАМЕТКИ"))

        if (rootNotes.isEmpty()) {
            items.add(StartListItem.EmptyNotes)
        } else {
            rootNotes.forEach { note ->
                items.add(StartListItem.NoteItem(note))
            }
        }

        items.add(StartListItem.AddNoteButton)

        return items
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNote(null) // Создаем заметку в корне
                // После создания можно перейти к редактированию
                // или просто обновить список
                loadData()
            } catch (e: Exception) {
                _message.value = "Ошибка создания заметки: ${e.message}"
            }
        }
    }

    fun createNewNotebook(name: String) {

        _isLoading.value = true

        viewModelScope.launch {
            try {
                val newNotebook = createNotebook(name) // Создаем записную книжку
                loadData()
                _message.value = "Записная книжка создана: ${newNotebook.name}"
            } catch (e: Exception) {
                _message.value = "Ошибка создания записной книжки: ${e.message}"
            }
            finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _message.value = null
    }
}