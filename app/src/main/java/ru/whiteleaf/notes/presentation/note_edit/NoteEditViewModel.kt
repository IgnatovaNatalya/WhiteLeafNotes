package ru.whiteleaf.notes.presentation.note_edit

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.SaveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.ShareNoteFileUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.repository.SecurityPreferences
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.use_case.CheckNotebookAccessUseCase
import ru.whiteleaf.notes.presentation.note_list.NotebookSecurityState

class NoteEditViewModel(
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val saveNoteUseCase: SaveNoteUseCase,
    private val shareNoteFileUseCase: ShareNoteFileUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val encryptionRepository: EncryptionRepository,
    private val securityPreferences: SecurityPreferences,
    private val noteId: String?,
    private val notebookPath: String?,
    private val checkNotebookAccessUseCase: CheckNotebookAccessUseCase,
) : ViewModel() {

    private val _noteEditState = MutableLiveData<NoteEditState>()
    val noteEditState: LiveData<NoteEditState> = _noteEditState

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _noteFile = MutableLiveData<Uri?>()
    val noteFile: LiveData<Uri?> = _noteFile

    private val _isLocked = MutableLiveData<Boolean>()
    val isLocked: LiveData<Boolean> = _isLocked

    private val _isEncryptedAndUnlocked = MutableLiveData<Boolean>()
    val isEncryptedAndUnlocked: LiveData<Boolean> = _isEncryptedAndUnlocked

    // Вычисляем состояние один раз при создании
    private val notebookSecurityState: NotebookSecurityState by lazy {
        val isEncrypted = notebookPath?.let {
            securityPreferences.isNotebookEncrypted(it)
        } ?: false

        val isUnlocked = notebookPath?.let {
            securityPreferences.isNotebookUnlocked(it) &&
                    encryptionRepository.isNotebookUnlocked(it)
        } ?: true

        NotebookSecurityState(
            isEncrypted = isEncrypted,
            isUnlocked = isUnlocked,
            requiresAuthentication = isEncrypted && !isUnlocked
        )
    }

    private val _noteMoved = MutableLiveData<Boolean>()
    val noteMoved: LiveData<Boolean> = _noteMoved

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isSaved = MutableLiveData<Boolean>()
    val isSaved: LiveData<Boolean> = _isSaved //todo сделать индикатор сохранения


    init {

        loadNoteWithSecurityCheck()
    }


    private fun loadNoteWithSecurityCheck() {
        if (noteId != null) viewModelScope.launch {
            try {

                // ВСЕГДА проверяем актуальное состояние при загрузке
                val isEncrypted = notebookPath?.let {
                    checkNotebookAccessUseCase.isNotebookEncrypted(it)
                } ?: false

                val hasAccess = notebookPath?.let {
                    checkNotebookAccessUseCase(it)
                } ?: true

                val note = getNoteUseCase(noteId, notebookPath)
                if (note == null) return@launch


                if (!hasAccess) {
                    _noteEditState.postValue(NoteEditState.Error("Заметка заблокирована. Разблокируйте записную книжку для редактирования."))
                } else if (isEncrypted) {
                    println("🔍 Проверяем ключ перед дешифровкой...")
                    encryptionRepository.debugKeyInfo(notebookPath)

                    // Разблокированный защищенный блокнот
                    encryptionRepository.decryptNote(noteId, notebookPath)
                    val decryptedContent = encryptionRepository.getDecryptedContent(noteId) ?: note.content
                    _noteEditState.postValue(NoteEditState.Success(note.copy(content = decryptedContent)))
                    _note.postValue(note.copy(content = decryptedContent))
                } else {
                    // Обычный блокнот
                    _noteEditState.postValue(NoteEditState.Success(note))
                    _note.postValue(note)
                }
            } catch (e: Exception) {
                _noteEditState.postValue(NoteEditState.Error("Ошибка загрузки заметки: ${e.message}"))
            }
        }
    }

//    fun loadNote() {
//        if (noteId != null) viewModelScope.launch {
//            try {
//                val note = getNoteUseCase(noteId, notebookPath)
//                if (note == null) return@launch
//
//                _note.postValue(note)
//
//                // Заблокированный блокнот
//                if (!hasAccess)
//                    _noteEditState.postValue(NoteEditState.Error("Разблокируйте записную книжку для редактирования"))
//
//                else if (isEncrypted) {  // Разблокированный защищенный блокнот
//                    encryptionRepository.decryptNote(noteId, notebookPath)
//                    val decryptedContent =
//                        encryptionRepository.getDecryptedContent(noteId) ?: note.content
//
//                    _noteEditState.postValue(NoteEditState.Success(note.copy(content = decryptedContent)))
//                } else { // Обычный блокнот
//                    _noteEditState.postValue(NoteEditState.Success(note))
//                }
//            } catch (e: Exception) {
//                _noteEditState.postValue(NoteEditState.Error("Ошибка загрузки заметки: ${e.message}"))
//            }
//        }
//    }



    fun updateNoteTitle(newTitle: String) {
        if (_isLocked.value == true) return

        val currentNote = _note.value ?: return


        viewModelScope.launch {
            try {
                val isEncrypted = notebookPath?.let {
                    checkNotebookAccessUseCase.isNotebookEncrypted(it)
                } ?: false



                if (isEncrypted) {
                    // Для защищенного блокнота - сохраняем в кэш
                    val currentContent = currentNote.content
                    encryptionRepository.cacheDecryptedContent(
                        currentNote.id,
                        currentContent,
                        newTitle
                    )
                    _note.postValue(currentNote.copy(content = currentContent))

                    // Переименовываем файл если название изменилось
                    if (newTitle != currentNote.title && newTitle.isNotEmpty()) {
                        val newNoteId = renameNoteUseCase(currentNote, newTitle)
                        _note.postValue(currentNote.copy(id = newNoteId, title = newTitle))
                    }
                } else {
                    // Обычный блокнот
                    if (newTitle.isNotEmpty() && newTitle != currentNote.title) {
                        val newNoteId = renameNoteUseCase(currentNote, newTitle)
                        _note.postValue(currentNote.copy(id = newNoteId, title = newTitle))
                    }
                }

            } catch (e: Exception) {
                showMessage("Ошибка при переименовании заметки: ${e.message}")
            }
        }
    }

    fun updateNoteContent(content: String) {
        if (_isLocked.value == true) return

        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                val isEncrypted = notebookPath?.let {
                    checkNotebookAccessUseCase.isNotebookEncrypted(it)
                } ?: false


                if (isEncrypted) {
                    // Для защищенного блокнота - сохраняем в кэш
                    val currentTitle = currentNote.title
                    encryptionRepository.cacheDecryptedContent(
                        currentNote.id,
                        content,
                        currentTitle
                    )
                } else {
                    // Обычный блокнот
                    val updatedNote = currentNote.copy(content = content)
                    saveNoteUseCase(updatedNote)
                }
            } catch (e: Exception) {
                showMessage("Ошибка при сохранении текста заметки: ${e.message}")
            }
        }
    }

    fun saveAndEncryptOnExit() {
        viewModelScope.launch {
            val currentNote = _note.value ?: return@launch
            val isEncrypted = notebookPath?.let {
                checkNotebookAccessUseCase.isNotebookEncrypted(it)
            } ?: false

            if (isEncrypted) {
                // Шифруем заметку при выходе
                encryptionRepository.encryptNote(currentNote.id, notebookPath)
            }
        }
    }

    fun updateFullNote(title: String, content: String) { ///
        showMessage("Сохранение заметки")

        viewModelScope.launch {
            try {
                updateNoteContent(content)
                updateNoteTitle(title)
            } catch (e: Exception) {
                showMessage("Ошибка сохранения заметки: ${e.message}")
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
                _message.postValue("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun refreshNote() = loadNoteWithSecurityCheck()

    private fun showMessage(msg: String) = _message.postValue(msg)

    fun clearMessage() = _message.postValue(null)

}

