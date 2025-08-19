package com.example.txtnotesapp.presentation.note_edit

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.GetNote
import com.example.txtnotesapp.domain.use_case.SaveNote
import kotlinx.coroutines.launch

class NoteEditViewModel(
    private val getNote: GetNote,
    private val saveNote: SaveNote,
    private val createNote: CreateNote,
    private val noteId: String?,
    private val notebookPath: String?
) : ViewModel() {

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

    init {
        loadNote()
    }

    private fun loadNote() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (noteId != null) {
                    // Загрузка существующей заметки
                    val existingNote = getNote(noteId, notebookPath)
                    if (existingNote != null) {
                        _note.value = existingNote
                    } else {
                        _error.value = "Заметка не найдена"
                    }
                } else {
                    // Создание новой заметки
                    val newNote = createNote(notebookPath)
                    _note.value = newNote
                }
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки заметки: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateNoteContent(content: String) {
        val currentNote = _note.value ?: return

        val updatedNote = currentNote.copy(content = content)
        _note.value = updatedNote

        // Автосохранение при изменении
        viewModelScope.launch {
            try {
                saveNote(updatedNote)
                _isSaved.postValue(true)
            } catch (e: Exception) {
                _error.postValue("Ошибка сохранения: ${e.message}")
                _isSaved.postValue(false)
            }
        }
    }

    fun updateNoteTitle(newTitle: String) {
        val currentNote = _note.value ?: return

        // TODO: Реализовать переименование файла
        // Это потребует изменения в репозитории для поддержки переименования
    }

    fun clearError() {
        _error.value = null
    }
}