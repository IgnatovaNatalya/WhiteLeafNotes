package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.SaveNoteContentUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNoteFileUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.interactor.SettingsInteractor
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.recent.RemoveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.recent.SaveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateFullNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase
import ru.whiteleaf.notes.presentation.note_list.NoteListState

class NoteEditViewModel(
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val saveNoteContentUseCase: SaveNoteContentUseCase,
    private val updateFullNoteUseCase: UpdateFullNoteUseCase,
    private val shareNoteFileUseCase: ShareNoteFileUseCase,
    private val updateNoteDateUseCase: UpdateNoteDateUseCase,
    private val noteId: String?,
    private val notebookPath: String?,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val settingsInteractor: SettingsInteractor,
    private val isNotebookProtectedUseCase: IsNotebookProtectedUseCase,
    private val saveRecentNoteUseCase: SaveRecentNoteUseCase,
    private val removeRecentNoteUseCase: RemoveRecentNoteUseCase,
    private val getNotebooksUseCase: GetNotebooksUseCase,
) : ViewModel() {

    private val _noteEditState = MutableLiveData<NoteEditState>()
    val noteEditState: LiveData<NoteEditState> = _noteEditState

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _isNotebookProtected = MutableLiveData<Boolean>()
    val isNotebookProtected: LiveData<Boolean> = _isNotebookProtected

    private val _noteFile = MutableLiveData<Uri?>()
    val noteFile: LiveData<Uri?> = _noteFile

    private val _navigateBack = MutableLiveData<Boolean>()
    val navigateBack: LiveData<Boolean> = _navigateBack

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isDateUpdating = MutableStateFlow(false)

    private var pendingSaveTitle: String? = null
    private var pendingSaveContent: String? = null

    private var currentScrollPosition: Int? = null

    private var notebookList: List<Notebook> = emptyList()

    init {
        loadNote()
        loadNotebooks()
    }

    private fun loadNotebooks() {
        viewModelScope.launch {
            try {
                notebookList = getNotebooksUseCase()
            } catch (e: Exception) {
                println("DEBUG: NoteEditVm: loadNotebooks: Error loading notebooks: ${e.message}")
            }
        }
    }

    fun getAllNotebooks(): List<Notebook> = notebookList

    fun reloadNotePosition() {
        val currentNote = _note.value

        if (currentNote != null) {
            _noteEditState.postValue(
                NoteEditState.Success(
                    currentNote,
                    scrollPosition = currentScrollPosition ?: 0,
                    isNotebookProtected.value == true
                )
            )
        } else loadNote()
    }

    private fun loadNote() {
        if (noteId != null) viewModelScope.launch {
            _noteEditState.postValue(NoteEditState.Loading)
            try {
                val note = getNoteUseCase(noteId, notebookPath)

                _note.value = note

                if (note.notebookPath != null)
                    _isNotebookProtected.value = isNotebookProtectedUseCase(note.notebookPath)

                if (currentScrollPosition == null) currentScrollPosition = getNoteScrollPosition()

                _noteEditState.postValue(
                    NoteEditState.Success(
                        note,
                        scrollPosition = currentScrollPosition ?: 0,
                        isNotebookProtected.value == true
                    )
                )

                if (note.isNotEmpty()) removeRecentNoteUseCase(note)

            } catch (e: AuthenticationRequiredException) {
                _noteEditState.postValue(NoteEditState.Blocked)
                println("DEBUG: NoteEditVM: Key not unlocked while loading note: ${e.message}")

            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error loading: ${e.message}")
                _noteEditState.postValue(NoteEditState.Error(e.message ?: "Ошибка загрузки"))
            }
        }
    }

    fun lockNote() {
        _noteEditState.value = NoteEditState.Blocked
    }

    fun updateNoteTitle(newTitle: String) {
        val currentNote = _note.value ?: return
        viewModelScope.launch {
            try {
                println("DEBUG: NoteEditVM: Updating note title to $newTitle, currentNote: ${_note.value?.printDebug()}")
                if (newTitle != currentNote.title) {
                    _note.postValue(renameNoteUseCase(currentNote, newTitle))
                }
            } catch (e: Exception) {
                showMessage("Ошибка при переименовании заметки: ${e.message}")
            }
        }

    }

    fun updateNoteContent(content: String) {
        val currentNote = _note.value ?: return
        println("DEBUG: NoteEditVM: Updating note content, current note: ${_note.value?.printDebug()}")
        viewModelScope.launch {
            try {
                val updatedNote = currentNote.copy(content = content)
                saveNoteContentUseCase(updatedNote)
                _note.postValue(updatedNote)
            } catch (e: AuthenticationRequiredException) {
                pendingSaveContent = content
                _noteEditState.value = NoteEditState.Blocked
                println("DEBUG: NoteEditVM: key not unlocked while updating: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка при сохранении текста заметки: ${e.message}")
                println("DEBUG: NoteEditVM: error: ${e.message}")
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
                _noteEditState.postValue(
                    NoteEditState.Success(
                        updatedNote,
                        scrollPosition,
                        _isNotebookProtected.value == true
                    )
                )

                _message.postValue("Дата заметки обновлена")
            } catch (e: AuthenticationRequiredException) {

                _noteEditState.value = NoteEditState.Blocked
                println("DEBUG: NoteEditVM: key not unlocked while date: ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка обновления даты: ${e.message}")
            } finally {
                _isDateUpdating.value = false
            }
        }
    }


    fun updateFullNote(title: String, content: String, onExit: Boolean) {
        val currentNote = _note.value ?: return
        showMessage("Сохранение заметки")
        println("DEBUG: NoteEditVM: Upd note on exit, title=$title, content=${content.take(10)}")

        viewModelScope.launch {
            try {
                val updatedNote = updateFullNoteUseCase(currentNote, title, content)

                if (onExit) _navigateBack.postValue(true)
                else _note.postValue(updatedNote)

            } catch (e: AuthenticationRequiredException) {
                if (onExit) _noteEditState.postValue(NoteEditState.ShowBiometricForSaveOnExit)
                else _noteEditState.postValue(NoteEditState.ShowBiometricForSave)

                println("DEBUG: NoteEditVM: Authentication required while updating full note onExit=$onExit: ${e.message}")

                if (content != currentNote.content) pendingSaveContent = content
                if (title != currentNote.title) pendingSaveTitle = title
            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error updating full note onExit=$onExit: ${e.message}")
                showMessage("Ошибка сохранения заметки: ${e.message}")
            }
        }
    }

    fun saveToRecent() {
        viewModelScope.launch {
            try {
                val currentNote = _note.value ?: return@launch
                saveRecentNoteUseCase(currentNote)
            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error saving note to recent: ${e.message}")
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

    fun moveNote(context: Context, targetNotebookPath: String) { ///
        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                val unlocked =

                    if (isNotebookProtectedUseCase(targetNotebookPath)) unlockNotebookUseCase(
                        targetNotebookPath, context, title = "Целевая записная книжка защищена",
                        reason = "Для перемещения"
                    ) else true

                if (unlocked) {
                    moveNoteUseCase(currentNote, targetNotebookPath)
                    _navigateBack.postValue(true)
                } else {
                    _noteEditState.value = NoteEditState.Blocked
                    showMessage("Ошибка разблокировки")
                }
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.value = NoteEditState.Blocked
                println("DEBUG: NoteEditVM:  AuthenticationRequiredException ${e.message}")
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
                _navigateBack.postValue(true)
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.value = NoteEditState.Blocked
                println("DEBUG: NoteEditVM: key not unlocked while deleting note: ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun saveNoteScrollPosition(scrollPosition: Int) {
        if (noteId != null && notebookPath != null) {
            println("DEBUG: NoteEditVM: saveNoteScrollPosition: noteId=$noteId, notebookPath=$notebookPath, pos=$scrollPosition")
            settingsInteractor.saveNoteScrollPosition(noteId, notebookPath, scrollPosition)
        }
    }

    fun rememberNoteScrollPosition(scrollPosition: Int) {
        println("DEBUG: NoteEditVM: rememberNoteScrollPosition: noteId=$noteId, notebookPath=$notebookPath, pos=$scrollPosition")
        currentScrollPosition = scrollPosition
    }

    fun getNoteScrollPosition(): Int {
        return if (noteId != null && notebookPath != null) {
            settingsInteractor.getNoteScrollPosition(noteId, notebookPath) ?: 0
        } else 0
    }

    fun unlockNotebook(context: Context, reason: String? = null) {
        viewModelScope.launch {
            val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                notebookPath,
                context,
                reason = reason ?: "Для редактирования"
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

    fun unlockAndSavePending(context: Context, thenExit: Boolean) {

        try {
            viewModelScope.launch {
                val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                    notebookPath,
                    context,
                    reason = if (thenExit) "Для сохранения перед выходом" else "Для продолжения операции"
                ) else true

                val currentNote = _note.value ?: return@launch

                if (unlocked) {
                    if (pendingSaveTitle != null) {
                        if (pendingSaveContent != null) {
                            _note.postValue(
                                updateFullNoteUseCase(
                                    currentNote, pendingSaveTitle!!, pendingSaveContent!!
                                )
                            )
                        } else _note.postValue(renameNoteUseCase(currentNote, pendingSaveTitle!!))
                    } else if (pendingSaveContent != null) {
                        val updatedNote = currentNote.copy(content = pendingSaveContent!!)
                        saveNoteContentUseCase(updatedNote)
                        _note.postValue(updatedNote)
                    }

                    if (thenExit) _navigateBack.postValue(true) //вызывает выход из фрагмента

                    pendingSaveContent = null
                    pendingSaveTitle = null

                } else {
                    _noteEditState.postValue(NoteEditState.Error("Не удалось разблокировать записную книжку"))
                }
            }
        } catch (e: AuthenticationRequiredException) {
            _noteEditState.value = NoteEditState.Blocked
            println("DEBUG: NoteEditVM: key not unlocked while unlockAndSavePending: ${e.message}")
        } catch (e: Exception) {
            _message.postValue("Не удалось сохранить ${e.message}")
        }
    }

    fun unlockAndFullSave(context: Context, title: String, content: String): Boolean {
        var saved = false
        try {
            viewModelScope.launch {
                val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                    notebookPath,
                    context,
                    reason = "Для экспорта"
                ) else true

                val currentNote = _note.value ?: return@launch

                saved = if (unlocked) {
                    if (title != currentNote.title)
                        if (content != currentNote.content) {
                            _note.postValue(
                                updateFullNoteUseCase(currentNote, title, content)
                            )
                        } else {
                            _note.postValue(renameNoteUseCase(currentNote, title))
                        }
                    else if (content != currentNote.content) {
                        val updatedNote = currentNote.copy(content = content)
                        saveNoteContentUseCase(updatedNote)
                        _note.postValue(updatedNote)
                    }
                    true
                } else {
                    _noteEditState.postValue(NoteEditState.Error("Не удалось разблокировать записную книжку"))
                    false
                }
            }
        } catch (e: AuthenticationRequiredException) {
            _noteEditState.value = NoteEditState.Blocked
            println("DEBUG: NoteEditVM: key not unlocked while unlockAndFullSave: ${e.message}")
        } catch (e: Exception) {
            _message.postValue("Не удалось сохранить ${e.message}")
        }
        return saved
    }

    private fun showMessage(msg: String) = _message.postValue(msg)

    fun clearMessage() = _message.postValue(null)
}

