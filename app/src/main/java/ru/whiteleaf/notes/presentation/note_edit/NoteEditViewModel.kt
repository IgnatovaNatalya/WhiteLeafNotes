package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.SaveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.ShareNoteFileUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.repository.PreferencesRepository
import ru.whiteleaf.notes.domain.use_case.UpdateNoteDateUseCase
import java.security.InvalidKeyException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class NoteEditViewModel(
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val shareNoteFileUseCase: ShareNoteFileUseCase,
    //private val encryptionRepositoryOld: EncryptionRepositoryOld,
    private val updateNoteDateUseCase: UpdateNoteDateUseCase,
    private val noteId: String?,
    private val notebookPath: String?,
    private val encryptionRepository: EncryptionRepository,
    //private val checkNotebookAccessOldUseCase: CheckNotebookAccessOldUseCase,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _noteEditState = MutableLiveData<NoteEditState>()
    val noteEditState: LiveData<NoteEditState> = _noteEditState

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _noteFile = MutableLiveData<Uri?>()
    val noteFile: LiveData<Uri?> = _noteFile

    //private val _isLocked = MutableLiveData<Boolean>()

    private val _noteMoved = MutableLiveData<Boolean>()
    val noteMoved: LiveData<Boolean> = _noteMoved

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

//    private val _isSaved = MutableLiveData<Boolean>()
//    val isSaved: LiveData<Boolean> = _isSaved

    private val _isDateUpdating = MutableStateFlow(false)

    private var pendingSaveContent: String? = null

    init {
        loadNote()
        //loadNoteWithSecurityCheck()
    }

    private fun loadNote() {
        if (noteId != null) viewModelScope.launch {
            _noteEditState.postValue(NoteEditState.Loading)
            try {
                val note = getNoteUseCase(noteId, notebookPath)
                if (note == null) return@launch
                _note.postValue(note)

                val scrollPosition = getNoteScrollPosition()

                _noteEditState.postValue(
                    NoteEditState.Success(
                        note,
                        scrollPosition = scrollPosition
                    )
                )
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteEditState.value = NoteEditState.NeedsBiometricForRead
                } else {
                    _noteEditState.value = NoteEditState.Error(e.message ?: "Ошибка загрузки")
                }
            }
        }
    }

//    private fun loadNoteWithSecurityCheck() {
//        if (noteId != null) viewModelScope.launch {
//            try {
//
//                // ВСЕГДА проверяем актуальное состояние при загрузке
//                val isEncrypted = notebookPath?.let {
//                    //checkNotebookAccessUseCase.isNotebookEncrypted(it)
//                    encryptionRepositoryOld.isNotebookUnlocked(notebookPath)
//                } ?: false
//
//                val hasAccess = notebookPath?.let {
//                    checkNotebookAccessOldUseCase(it)
//                } ?: true
//
//                val note = getNoteUseCase(noteId, notebookPath)
//                if (note == null) return@launch
//
//                val scrollPosition = getNoteScrollPosition()
//
//                if (!hasAccess) {
//                    _noteEditState.postValue(NoteEditState.Error("Заметка заблокирована. Разблокируйте записную книжку для редактирования."))
//                } else if (isEncrypted) {
//                    println("🔍 Проверяем ключ перед дешифровкой...")
//                    encryptionRepositoryOld.debugKeyInfo(notebookPath)
//
//                    // Разблокированный защищенный блокнот
//                    encryptionRepositoryOld.decryptNote(noteId, notebookPath)
//                    val decryptedContent =
//                        encryptionRepositoryOld.getDecryptedContent(noteId) ?: note.content
//                    _noteEditState.postValue(NoteEditState.Success(
//                        note.copy(content = decryptedContent,),
//                        scrollPosition = scrollPosition
//                    ))
//                    _note.postValue(note.copy(content = decryptedContent))
//                } else {
//                    // Обычный блокнот
//                    _noteEditState.postValue(NoteEditState.Success(note, scrollPosition))
//                    _note.postValue(note)
//                }
//            } catch (e: Exception) {
//                _noteEditState.postValue(NoteEditState.Error("Ошибка загрузки заметки: ${e.message}"))
//            }
//        }
//    }

    fun updateNoteTitle(newTitle: String) {
        //if (_isLocked.value == true) return
        println("DEBUG: Updating note title, title = $newTitle")

        val currentNote = _note.value ?: return
        println("DEBUG: Updating note title, currentNote = ${_note.value }")

        viewModelScope.launch {
            try {
//                val isEncrypted = notebookPath?.let {
//                    checkNotebookAccessOldUseCase.isNotebookEncrypted(it)
//                } ?: false


//                if (isEncrypted) {
//                    // Для защищенного блокнота - сохраняем в кэш
//                    val currentContent = currentNote.content
//                    encryptionRepositoryOld.cacheDecryptedContent(
//                        currentNote.id,
//                        currentContent,
//                        newTitle
//                    )
//                    _note.postValue(currentNote.copy(content = currentContent))
//
//                    // Переименовываем файл если название изменилось
//                    if (newTitle != currentNote.title && newTitle.isNotEmpty()) {
//                        val newNoteId = renameNoteUseCase(currentNote, newTitle)
//                        _note.postValue(currentNote.copy(id = newNoteId, title = newTitle))
//                    }
//                } else {

                if (newTitle.isNotEmpty() && newTitle != currentNote.title) {
                    val newNoteId = renameNoteUseCase(currentNote, newTitle)
                    _note.postValue(currentNote.copy(id = newNoteId, title = newTitle))
                }


            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteEditState.value = NoteEditState.NeedsBiometricForSave
                } else
                    showMessage("Ошибка при переименовании заметки: ${e.message}")
            }
        }
    }

    fun updateNoteContent(content: String) {
        //if (_isLocked.value == true) return
        println("DEBUG: Updating note content, content =$content")
        val currentNote = _note.value ?: return
        println("DEBUG: Updating note content, current note= ${_note.value}")
        viewModelScope.launch {
            try {
//                val isEncrypted = notebookPath?.let {
//                    checkNotebookAccessOldUseCase.isNotebookEncrypted(it)
//                } ?: false


//                if (isEncrypted) {
//                    // Для защищенного блокнота - сохраняем в кэш
//                    val currentTitle = currentNote.title
//                    encryptionRepositoryOld.cacheDecryptedContent(
//                        currentNote.id,
//                        content,
//                        currentTitle
//                    )
//                } else {

                val updatedNote = currentNote.copy(content = content)
                saveNoteUseCase(updatedNote)
                _note.postValue(updatedNote)
                //}
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    pendingSaveContent = content
                    _noteEditState.value = NoteEditState.NeedsBiometricForSave
                } else
                    showMessage("Ошибка при сохранении текста заметки: ${e.message}")
            }
        }
    }

    fun updateNoteDate(newDate: Long) {

        viewModelScope.launch {

            val currentNote = _note.value ?: return@launch
            val updatedNote = currentNote.copy(modifiedAt = newDate)

            _isDateUpdating.value = true

            try {
                updateNoteDateUseCase(currentNote, newDate)

                val scrollPosition = getNoteScrollPosition()

                _note.value = updatedNote
                _noteEditState.postValue(NoteEditState.Success(updatedNote, scrollPosition))

                _message.postValue("Дата заметки обновлена")

            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteEditState.value = NoteEditState.NeedsBiometricForSave
                } else
                    _message.postValue("Ошибка обновления даты: ${e.message}")
            } finally {
                _isDateUpdating.value = false
            }
        }
    }

    // Форматирует дату для отображения
    fun formatDate(timestamp: Long): String {
        return try {
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("ru"))
            formatter.format(date)
        } catch (e: Exception) {
            "Дата не указана"
        }
    }

//    fun saveAndEncryptOnExit() {
//        viewModelScope.launch {
//            val currentNote = _note.value ?: return@launch
//            val isEncrypted = notebookPath?.let {
//                checkNotebookAccessOldUseCase.isNotebookEncrypted(it)
//            } ?: false
//
//            if (isEncrypted) {
//                // Шифруем заметку при выходе
//                encryptionRepositoryOld.encryptNote(currentNote.id, notebookPath)
//            }
//        }
//    }

    fun updateFullNote(title: String, content: String) { ///
        showMessage("Сохранение заметки")
        println("DEBUG: Updating full note title=$title, content=$content")

        viewModelScope.launch {
            try {
                updateNoteContent(content)
                updateNoteTitle(title)
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteEditState.value = NoteEditState.NeedsBiometricForSave
                } else {
                    println("DEBUG: Error updating full note")
                    showMessage("Ошибка сохранения заметки: ${e.message}")
                }
            }
        }
    }

    fun shareNoteFile() { ///
        val currentNote = _note.value ?: return
        viewModelScope.launch {
            try {
                val file = shareNoteFileUseCase(currentNote)
                _noteFile.postValue(file)
            } catch (e: Exception) {
                _message.postValue("Ошибка передачи файла заметки: ${e.message}")
            }
        }
    }

    fun moveNote(notebookTitle: String) { ///
        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                moveNoteUseCase(currentNote, notebookTitle)
                _noteMoved.postValue(true)
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteEditState.value = NoteEditState.NeedsBiometricForSave
                } else
                    _message.postValue("Ошибка перемещения: ${e.message}")
            }
        }

    }

    fun deleteNote() {
        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                deleteNoteUseCase(currentNote)
                _noteMoved.postValue(true)
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteEditState.value = NoteEditState.NeedsBiometricForSave
                } else
                    _message.postValue("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun saveNoteScrollPosition(scrollPosition: Int) {
        if (noteId != null && notebookPath != null)
            preferencesRepository.saveNoteScrollPosition(noteId, notebookPath, scrollPosition)
    }

    fun getNoteScrollPosition(): Int {
        if (noteId != null && notebookPath != null) {
            val pos = preferencesRepository.getNoteScrollPosition(noteId, notebookPath)
            return pos ?: 0
        } else return 0
    }

    fun onBiometricSuccess(context: Context) {
        viewModelScope.launch {
            val unlocked = if (notebookPath != null) encryptionRepository.unlockNotebook(
                notebookPath,
                context
            ) else true
            if (unlocked) {
                if (pendingSaveContent != null) {
                    updateNoteContent(pendingSaveContent!!)
                    pendingSaveContent = null
                } else {
                    loadNote()
                }
            } else {
                _noteEditState.postValue(NoteEditState.Error("Не удалось разблокировать записную книжку"))
            }
        }
    }

    fun lockNotebook() {
        if (notebookPath != null) {
            encryptionRepository.lockNotebook(notebookPath)
        }
    }

    fun refreshNote() = loadNote() //loadNoteWithSecurityCheck()

    private fun showMessage(msg: String) = _message.postValue(msg)

    fun clearMessage() = _message.postValue(null)

}

