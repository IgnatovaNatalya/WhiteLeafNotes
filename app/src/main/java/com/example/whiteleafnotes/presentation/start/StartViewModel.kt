package com.example.whiteleafnotes.presentation.start

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook
import com.example.whiteleafnotes.domain.use_case.CreateNoteUseCase
import com.example.whiteleafnotes.domain.use_case.CreateNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.DeleteNoteUseCase
import com.example.whiteleafnotes.domain.use_case.DeleteNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.GetNotebooksUseCase
import com.example.whiteleafnotes.domain.use_case.GetNotesUseCase
import com.example.whiteleafnotes.domain.use_case.MoveNoteUseCase
import com.example.whiteleafnotes.domain.use_case.RenameNoteUseCase
import com.example.whiteleafnotes.domain.use_case.RenameNotebookUseCase
import com.example.whiteleafnotes.domain.use_case.ShareNotebookUseCase
import kotlinx.coroutines.launch
import kotlin.collections.forEach

class StartViewModel(
    private val getNotebooksUseCase: GetNotebooksUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val createNotebookUseCase: CreateNotebookUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val deleteNotebookUseCase: DeleteNotebookUseCase,
    private val shareNotebookUseCase: ShareNotebookUseCase
) : ViewModel() {

    private val _startItems = MutableLiveData<List<StartListItem>>()
    val startItems: LiveData<List<StartListItem>> = _startItems

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _navigateToCreatedNote = MutableLiveData<Note?>()
    val navigateToCreatedNote: LiveData<Note?> = _navigateToCreatedNote

    private val _navigateToCreatedNotebook = MutableLiveData<Notebook?>()
    val navigateToCreatedNotebook: LiveData<Notebook?> = _navigateToCreatedNotebook

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _uriToShare = MutableLiveData<Uri?>()
    val uriToShare: LiveData<Uri?> = _uriToShare

    init {
        loadData()
    }

    fun loadData() {
        _isLoading.value = true

        viewModelScope.launch {
            try {
                val notebooks = getNotebooksUseCase()
                val rootNotes = getNotesUseCase(null) // Заметки в корневой папке

                val items = buildStartItems(notebooks, rootNotes)
                _startItems.value = items
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildStartItems(
        notebooks: List<Notebook>,
        rootNotes: List<Note>
    ): List<StartListItem> {
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
        items.add(StartListItem.Spacing)

        return items
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(null)
                _navigateToCreatedNote.value = newNote
                _message.value = "Заметка создана"
            } catch (e: Exception) {
                _message.value = "Ошибка создания заметки: ${e.message}"
            }
        }
    }

    fun createNewNotebook(name: String) {
        viewModelScope.launch {
            try {
                val newNotebook = createNotebookUseCase(name)
                _navigateToCreatedNotebook.value = newNotebook
                _message.value = "Записная книжка создана: ${newNotebook.name}"
            } catch (e: Exception) {
                _message.value = "Ошибка создания записной книжки: ${e.message}"
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNoteUseCase(note, newTitle)
                    loadData()
                    _message.postValue("Название заметки изменено")
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun renameNotebook(notebook: Notebook, newName: String) {
        viewModelScope.launch {
            try {
                if (newName != notebook.name) {
                    renameNotebookUseCase(notebook, newName)
                    loadData()
                    _message.postValue("Название записной книжки изменено")
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun moveNote(note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                moveNoteUseCase(note, targetNotebookPath)
                loadData()
            } catch (e: Exception) {
                _message.postValue("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                loadData()
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    fun shareNotebook(notebookPath: String) {
        _message.postValue("Создание архива...")
        viewModelScope.launch {
            try {
                val result = shareNotebookUseCase(notebookPath)
                if (result.isSuccess) {
                    _uriToShare.postValue(result.getOrNull())
                    _message.postValue("Архив создан успешно")
                } else
                    _message.postValue(result.exceptionOrNull()?.message ?: "Ошибка экспорта")
            } catch (e: Exception) {
                _message.postValue("Ошибка передачи архива записной книжки: ${e.message}")
            }
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            try {
                deleteNotebookUseCase(notebook)
                loadData()
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления записной книжки: ${e.message}")
            }
        }
    }

    fun onNotebookShared() = _uriToShare.postValue(null)

    fun onNoteNavigated() = _navigateToCreatedNote.postValue(null)

    fun onNotebookNavigated() = _navigateToCreatedNotebook.postValue(null)

    fun clearMessage() = _message.postValue(null)

    fun reloadNotes() = loadData()

}