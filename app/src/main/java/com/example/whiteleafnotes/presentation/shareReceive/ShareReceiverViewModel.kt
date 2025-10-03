package com.example.whiteleafnotes.presentation.shareReceive

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.whiteleafnotes.domain.model.SharedContent
import com.example.whiteleafnotes.domain.model.SharedContentResult
import com.example.whiteleafnotes.domain.use_case.GetSharedContentUseCase
import com.example.whiteleafnotes.domain.use_case.InsertNoteUseCase
import kotlinx.coroutines.launch

class ShareReceiverViewModel(
    private val getSharedContent: GetSharedContentUseCase,
    private val insertNoteUseCase: InsertNoteUseCase

) : ViewModel() {

    private val _content = MutableLiveData<SharedContent>()
    val content: LiveData<SharedContent> = _content

    private val _contentState = MutableLiveData<SharedContentResult<SharedContent>>()
    val contentState: LiveData<SharedContentResult<SharedContent>> = _contentState

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved


    fun processIntent(intent: Intent) {
        viewModelScope.launch {
            _contentState.postValue(getSharedContent.execute(intent))
        }
    }

    fun insertNote(receivedTitle: String, receivedText: String) {
        _isSaved.postValue(false)

        viewModelScope.launch {
            try {
                insertNoteUseCase(receivedTitle, receivedText, "")
                _isSaved.postValue(true)
            } catch (e: Exception) {
                _message.postValue("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.postValue(null)
    }
}