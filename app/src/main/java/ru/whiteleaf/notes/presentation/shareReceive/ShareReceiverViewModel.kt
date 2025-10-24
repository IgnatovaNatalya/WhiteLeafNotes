package ru.whiteleaf.notes.presentation.shareReceive

import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.SharedContent
import ru.whiteleaf.notes.domain.model.SharedContentResult
import ru.whiteleaf.notes.domain.use_case.GetSharedContentUseCase
import ru.whiteleaf.notes.domain.use_case.InsertNoteUseCase
import kotlinx.coroutines.launch

class ShareReceiverViewModel(
    private val getSharedContent: GetSharedContentUseCase,
    private val insertNoteUseCase: InsertNoteUseCase

) : ViewModel() {

    private val _content = MutableLiveData<SharedContent>()
    val content: LiveData<SharedContent> = _content

    private val _contentState = MutableLiveData<SharedContentResult<SharedContent>>()
    val contentState: LiveData<SharedContentResult<SharedContent>> = _contentState

    private val _noteCreated = MutableLiveData<String>()
    val noteCreated: LiveData<String> = _noteCreated

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    fun processIntent(intent: Intent) {
        viewModelScope.launch {
            _contentState.postValue(getSharedContent.execute(intent))
        }
    }

    fun insertNote(receivedTitle: String, receivedText: String) {

        viewModelScope.launch {
            try {
                val note = insertNoteUseCase(receivedTitle, receivedText, "")
                _noteCreated.postValue(note.id)
            } catch (e: Exception) {
                _message.postValue("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun clearMessage() {
        _message.postValue(null)
    }
}