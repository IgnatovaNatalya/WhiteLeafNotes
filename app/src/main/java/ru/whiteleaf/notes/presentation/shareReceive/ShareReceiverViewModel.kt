package ru.whiteleaf.notes.presentation.shareReceive

import android.content.Intent
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.common.utils.ImageStorageUtils
import ru.whiteleaf.notes.domain.model.SharedContent
import ru.whiteleaf.notes.domain.model.SharedContentResult
import ru.whiteleaf.notes.domain.use_case.share.GetSharedContentUseCase
import ru.whiteleaf.notes.domain.use_case.notes.InsertNoteUseCase
import kotlinx.coroutines.launch

class ShareReceiverViewModel(
    private val getSharedContent: GetSharedContentUseCase,
    private val insertNoteUseCase: InsertNoteUseCase,
    private val context: android.content.Context
) : ViewModel() {

    private val _content = MutableLiveData<SharedContent>()
    val content: LiveData<SharedContent> = _content

    private val _contentState = MutableLiveData<SharedContentResult<SharedContent>>()
    val contentState: LiveData<SharedContentResult<SharedContent>> = _contentState

    private val _noteCreated = MutableLiveData<String>()
    val noteCreated: LiveData<String> = _noteCreated

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private var _imageUri: Uri? = null
    val imageUri: Uri? get() = _imageUri

    fun processIntent(intent: Intent) {
        viewModelScope.launch {
            _contentState.postValue(getSharedContent.execute(intent))
        }
    }

    fun setImageUri(uri: Uri) {
        _imageUri = uri
    }

    fun insertNote(receivedTitle: String, receivedText: String) {

        viewModelScope.launch {
            try {
                // Если есть изображение, сохраняем его и вставляем маркер
                val finalText = if (_imageUri != null) {
                    val filename = ImageStorageUtils.saveImageFromUri(
                        context,
                        _imageUri!!,
                        "share_${System.currentTimeMillis()}",
                        null
                    )
                    val marker = filename?.let { "[IMG:$it]" } ?: ""
                    if (marker.isNotEmpty()) {
                        receivedText + "\n$marker"
                    } else {
                        receivedText
                    }
                } else {
                    receivedText
                }

                val note = insertNoteUseCase(receivedTitle, finalText, "")
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