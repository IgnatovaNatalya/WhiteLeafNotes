package com.example.txtnotesapp.presentation.note_list

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.use_case.CreateNote
import com.example.txtnotesapp.domain.use_case.DeleteNoteUseCase
import com.example.txtnotesapp.domain.use_case.GetNotes
import com.example.txtnotesapp.domain.use_case.MoveNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNote
import kotlinx.coroutines.launch
import java.io.IOException

class NoteListViewModel(
    private val getNotes: GetNotes,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val createNote: CreateNote,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNote: RenameNote,
    private val preferences: SharedPreferences,
    private val notebookPath: String?
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

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
        _isLoading.value = true
        _message.value = null

        viewModelScope.launch {
            try {
                val notesList = getNotes(notebookPath)
                _notes.value = notesList
            } catch (e: IOException) {
                _message.value = "Ошибка загрузки заметок: ${e.message}"
                _notes.value = emptyList()
            } catch (e: Exception) {
                _message.value = "Неизвестная ошибка: ${e.message}"
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
                loadNotes()
                _navigateToCreatedNote.value = newNote.title
            } catch (e: Exception) {
                _message.value = "Ошибка создания заметки: ${e.message}"
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                // Обновляем список после удаления
                loadNotes()
            } catch (e: Exception) {
                _message.value = "Ошибка удаления заметки: ${e.message}"
            }
        }
    }

    fun moveNote(note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                moveNoteUseCase(note, targetNotebookPath)
                loadNotes()
            } catch (e: Exception) {
                _message.value = "Ошибка перемещения заметки: ${e.message}"
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNote(note, newTitle)
                    loadNotes()
                    _message.postValue("Название заметки изменено")
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun onNoteClicked(noteId: String) = _navigateToNote.postValue( noteId)

    fun onNoteNavigated() = _navigateToNote.postValue(null)

    fun onNoteCreatedNavigated() =_navigateToCreatedNote.postValue(null)

    fun clearError() =  _message.postValue(null)

    private fun saveLastOpenNotebook() {
        preferences.edit {
            putString("last_notebook_path", notebookPath)
        }
    }

    fun reloadNotes() {
        loadNotes()
    }
}