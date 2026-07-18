package ru.whiteleaf.notes.presentation.start

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.CreateNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.recent.GetRecentNotesUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
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
    private val deleteNotebookUseCase: DeleteNotebookByPathUseCase,
    private val shareNotebookUseCase: ShareNotebookUseCase,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val isNotebookProtectedUseCase: IsNotebookProtectedUseCase,
    private val getRecentNotesUseCase: GetRecentNotesUseCase,
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
                val recent = getRecentNotesUseCase()
                val notebooks = getNotebooksUseCase()
                val rootNotes = getNotesUseCase(null) // Заметки в корневой папке

                rootNotes.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        _message.postValue("Пустая заметка удалена")
                    }
                }

                val items = buildStartItems(recent, notebooks, rootNotes.filter { it.isNotEmpty() })
                _startItems.value = items
            } catch (e: Exception) {
                _message.value = "Ошибка загрузки данных: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun buildStartItems(
        recent: List<RecentNote>,
        notebooks: List<Notebook>,
        rootNotes: List<Note>
    ): List<StartListItem> {
        val items = mutableListOf<StartListItem>()

        // Секция Недавние
        if (recent.isNotEmpty()) {
            items.add(StartListItem.Header("НЕДАВНИЕ"))
            recent.forEach { note ->
                items.add(StartListItem.RecentNoteItem(note))
            }
        }

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

    fun renameNotebook(notebook: Notebook, newName: String, context: Context) {
        viewModelScope.launch {
            try {
                val unlocked =
                    unlockNotebookUseCase(notebook.path, context, reason = "Для переименования")

                if (!unlocked) {
                    _message.postValue("Не удалось подтвердить личность")
                    return@launch
                }

                if (newName != notebook.name) {
                    renameNotebookUseCase(notebook.path, newName)
                    loadData()
                    _message.postValue("Название записной книжки изменено")
                }
            } catch (e: AuthenticationRequiredException) {
                _message.postValue("Записная книжка заблокирована: ${e.message}")
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

    fun deleteNotebook(notebook: Notebook, context: Context) {
        viewModelScope.launch {
            try {

                val unlocked = if (isNotebookProtectedUseCase(notebook.path)) {
                    unlockNotebookUseCase(notebook.path, context, reason = "Для удаления")
                } else
                    true

                if (unlocked) {
                    deleteNotebookUseCase(notebook.path)
                    loadData()
                    _message.postValue("Записная книжка удалена")
                } else {
                    _message.postValue("Не удалось подтвердить личность")
                    return@launch
                }

            } catch (e: AuthenticationRequiredException) {
                _message.postValue("Записная книжка заблокирована: ${e.message}")
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