package com.example.txtnotesapp.presentation.note_list

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.use_case.GetNotes
import kotlinx.coroutines.launch

class NoteListViewModel(
    private val getNotes: GetNotes,
    private val preferences: SharedPreferences,
    notebookPath: String?
): ViewModel() {
    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    init {
        loadNotes(notebookPath)
        saveLastOpenNotebook(notebookPath)
    }

    private fun loadNotes(notebookPath: String?) {
        viewModelScope.launch {
            _notes.value = getNotes(notebookPath)
        }
    }

    private fun saveLastOpenNotebook(path: String?) {
        preferences.edit {
            putString("last_notebook", path)
        }
    }
}