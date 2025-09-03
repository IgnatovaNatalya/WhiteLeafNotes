package com.example.txtnotesapp.presentation.note_list

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.DeleteNote
import com.example.txtnotesapp.domain.use_case.GetNotes
import com.example.txtnotesapp.domain.use_case.MoveNote
import kotlinx.coroutines.launch
import java.io.IOException

class NoteListViewModel(
    private val getNotes: GetNotes,
    private val deleteNote: DeleteNote,
    private val createNote: CreateNote,
    private val moveNote: MoveNote,
    private val preferences: SharedPreferences,
    private val notebookPath: String?
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _navigateToNote = MutableLiveData<String?>()
    val navigateToNote: LiveData<String?> = _navigateToNote

    private val _navigateToCreatedNote = MutableLiveData<String?>()
    val navigateToCreatedNote: LiveData<String?> = _navigateToCreatedNote

    init {
        loadNotes()
        saveLastOpenNotebook()
    }

    //    старый метод
//    fun loadNotes() {
//        _isLoading.value = true
//        _error.value = null
//
//        viewModelScope.launch {
//            try {
//                val notesList = getNotes(notebookPath)
//                _notes.value = notesList
//            } catch (e: Exception) {
//                _error.value = "Ошибка загрузки заметок: ${e.message}"
//                _notes.value = emptyList()
//            } finally {
//                _isLoading.value = false
//            }
//        }
//    }
    fun loadNotes() { //todo нужна аналогичная проверка в остальных методах
        _isLoading.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val notesList = getNotes(notebookPath)
                _notes.value = notesList
            } catch (e: IOException) {
                if (e.message?.contains("доступ") == true) {
                    _error.value = "Нет доступа к хранилищу. Проверьте разрешения."
                } else {
                    _error.value = "Ошибка загрузки заметок: ${e.message}"
                }
                _notes.value = emptyList()
            } catch (e: Exception) {
                _error.value = "Ошибка загрузки заметок: ${e.message}"
                _notes.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNote(notebookPath)
                _navigateToCreatedNote.value = newNote.title
            } catch (e: Exception) {
                _error.value = "Ошибка создания заметки: ${e.message}"
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNote(note)
                // Обновляем список после удаления
                loadNotes()
            } catch (e: Exception) {
                _error.value = "Ошибка удаления заметки: ${e.message}"
            }
        }
    }

    fun moveNote(note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                moveNote(note, targetNotebookPath)
                // Обновляем список после перемещения
                loadNotes()
            } catch (e: Exception) {
                _error.value = "Ошибка перемещения заметки: ${e.message}"
            }
        }
    }

    fun onNoteClicked(noteId: String) {
        _navigateToNote.value = noteId
    }

    fun onNoteNavigated() {
        _navigateToNote.value = null
    }

    fun onNoteCreatedNavigated() {
        _navigateToCreatedNote.value = null
    }

    fun clearError() {
        _error.value = null
    }

    private fun saveLastOpenNotebook() {
        preferences.edit {
            putString("last_notebook_path", notebookPath)
        }
    }
}