package com.example.txtnotesapp.presentation.note_edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.use_case.CreateNoteUseCase
import com.example.txtnotesapp.domain.use_case.GetNoteUseCase
import com.example.txtnotesapp.domain.use_case.MoveNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNoteUseCase
import com.example.txtnotesapp.domain.use_case.SaveNoteUseCase
import kotlinx.coroutines.launch

class NoteEditViewModel(
    private val getNoteUseCase: GetNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val noteId: String?,
    private val notebookPath: String?
) : ViewModel() {

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            _isLoading.value = true
            _message.value = null

            try {
                if (noteId != null) {
                    // Загрузка существующей заметки
                    val existingNote = getNoteUseCase(noteId, notebookPath)
                    if (existingNote != null) {
                        _note.postValue(existingNote)
                    } else {
                        _message.postValue("Заметка не найдена")
                    }
                } else {
                    // Создание новой заметки
                    val newNote = createNoteUseCase(notebookPath)
                    _note.postValue(newNote)
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка загрузки заметки: ${e.message}")
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun updateFullNote(title: String, content: String) {
        _message.postValue("Сохраняем всю заметку")
        viewModelScope.launch {
            try {
                updateNoteContent(content)
                updateNoteTitle(title)
            } catch (e: Exception) {
                _message.postValue("Ошибка сохранения заметки: ${e.message}")
                _isSaved.postValue(false)
            }
        }
    }

    fun updateNoteTitle(newTitle: String) {
        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                if (newTitle != currentNote.title && newTitle != "") {
                    val newNoteId = renameNoteUseCase(currentNote, newTitle)
                    _isSaved.postValue(true)
                    _message.postValue("Название заметки изменено")
                    _note.postValue(currentNote.copy(id = newNoteId, title = newTitle))
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования заметки: ${e.message}")
                _isSaved.postValue(false)
            }
        }
    }

    fun updateNoteContent(content: String) {
        val currentNote = _note.value ?: return

        val updatedNote = currentNote.copy(content = content)
        _note.postValue(updatedNote)

        // Автосохранение при изменении
        viewModelScope.launch {
            try {
                saveNoteUseCase(updatedNote)
                _isSaved.postValue(true)
            } catch (e: Exception) {
                _message.postValue("Ошибка сохранения: ${e.message}")
                _isSaved.postValue(false)
            }
        }
    }

    fun moveNote(notebookTitle: String) {
        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                moveNoteUseCase(currentNote, notebookTitle)
            } catch (e: Exception) {
                _message.postValue("Ошибка перемещения: ${e.message}")
                _isSaved.postValue(false)
            }
        }
    }

    fun clearMessage() {
        _message.postValue(null)
    }
}

