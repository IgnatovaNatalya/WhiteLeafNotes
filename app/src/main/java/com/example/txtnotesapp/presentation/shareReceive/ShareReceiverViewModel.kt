package com.example.txtnotesapp.presentation.shareReceive

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.txtnotesapp.domain.use_case.CreateNoteUseCase
import com.example.txtnotesapp.domain.use_case.RenameNoteUseCase
import com.example.txtnotesapp.domain.use_case.SaveNoteUseCase
import kotlinx.coroutines.launch

class ShareReceiverViewModel(
    private val createNoteUseCase: CreateNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase
) : ViewModel() {

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved

//    init{
//        _isSaved.postValue(false)
//    }

    fun insertNote(receivedTitle:String, receivedText:String) {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase()
                saveNoteUseCase(newNote.copy(content =receivedText))
                if (receivedTitle != "") renameNoteUseCase(newNote, receivedTitle)
                _isSaved.postValue(true)

            } catch (e: Exception) {
                _message.postValue("Ошибка сохранения заметки: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.postValue(null)
    }
}