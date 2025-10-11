package ru.whiteleaf.notes.presentation.note_list

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.ShareNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNotebookByPathUseCase
import ru.whiteleaf.notes.presentation.settings.ExportState
import kotlinx.coroutines.launch
import java.io.IOException

class NoteListViewModel(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val renameNotebookUseCase: RenameNotebookByPathUseCase,
    private val shareNotebookUseCase: ShareNotebookUseCase,
    private val deleteNotebookUseCase: DeleteNotebookByPathUseCase,
    private val preferences: SharedPreferences,
    private val notebookPath: String?
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    private val _shareNotebookState = MutableLiveData<ExportState>()
    val shareNotebookState: LiveData<ExportState> = _shareNotebookState

    private val _notebookRenamed = MutableLiveData<String>()
    val notebookRenamed: LiveData<String> = _notebookRenamed

    private val _notebookDeleted = MutableLiveData<Boolean>()
    val notebookDeleted: LiveData<Boolean> = _notebookDeleted

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _navigateToNote = MutableLiveData<String?>()
    val navigateToNote: LiveData<String?> = _navigateToNote

    private val _navigateToCreatedNote = MutableLiveData<String?>()
    val navigateToCreatedNote: LiveData<String?> = _navigateToCreatedNote

    init {
        loadNotes()
        saveLastOpenNotebook()
    }

    fun loadNotes() {
        _isLoading.postValue(true)
        _message.postValue(null)

        viewModelScope.launch {
            try {
                val notesList = getNotesUseCase(notebookPath)

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        _message.postValue("Пустая заметка удалена")
                    }
                }
                _notes.value = notesList.filter { it.isNotEmpty() }
            } catch (e: IOException) {
                _message.postValue("Ошибка загрузки заметок: ${e.message}")
                _notes.postValue(emptyList())
            } catch (e: Exception) {
                _message.postValue("Неизвестная ошибка: ${e.message}")
                _notes.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }


    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(notebookPath)
                _navigateToCreatedNote.postValue(newNote.id)
            } catch (e: Exception) {
                _message.postValue("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                loadNotes()
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    fun moveNote(note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                moveNoteUseCase(note, targetNotebookPath)
                loadNotes()
            } catch (e: Exception) {
                _message.postValue("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNoteUseCase(note, newTitle)
                    loadNotes()
                    _message.postValue("Название заметки изменено")
                    reloadNotes()
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun renameNotebook(newName: String) {
        viewModelScope.launch {
            try {
                if (newName != notebookPath && notebookPath != null) {
                    renameNotebookUseCase(notebookPath, newName)
                    _message.postValue("Название записной книжки изменено")
                    _notebookRenamed.postValue(newName)
                } else _message.postValue("Ошибка переименования")
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun shareNotebook() {
        _shareNotebookState.postValue(ExportState.Loading)

        viewModelScope.launch {
            if (notebookPath != null)
                try {
                    val result = shareNotebookUseCase(notebookPath)
                    if (result.isSuccess)
                        _shareNotebookState.postValue(ExportState.Success(result.getOrNull()))
                    else
                        _shareNotebookState.postValue(
                            ExportState.Error(
                                result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        )

                } catch (e: Exception) {
                    _message.postValue("Ошибка передачи файла записной книжки: ${e.message}")
                }
        }
    }

    fun deleteNotebook() {
        viewModelScope.launch {
            try {
                if (notebookPath != null) {
                    deleteNotebookUseCase(notebookPath)
                    _notebookDeleted.postValue(true)
                    _message.postValue("Записная книжка удалена")
                } else _message.postValue("Ошибка удаления записной книжки: путь не задан")
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления записной книжки: ${e.message}")
            }
        }
    }

    fun onNoteClicked(noteId: String) = _navigateToNote.postValue(noteId)

    fun onNoteNavigated() = _navigateToNote.postValue(null)

    fun onNoteCreatedNavigated() = _navigateToCreatedNote.postValue(null)

    fun clearMessage() = _message.postValue(null)

    private fun saveLastOpenNotebook() {
        preferences.edit {
            putString("last_notebook_path", notebookPath)
        }
    }

    fun reloadNotes() {
        loadNotes()
    }
}