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
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.recent.RemoveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.recent.SaveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateFullNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase

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
    private val removeRecentNoteUseCase: RemoveRecentNoteUseCase
) : ViewModel() {

    private val _noteEditState = MutableLiveData<NoteEditState>()
    val noteEditState: LiveData<NoteEditState> = _noteEditState

    private val _note = MutableLiveData<Note>()
    val note: LiveData<Note> = _note

    private val _isNotebookProtected = MutableLiveData<Boolean>()
    val isNotebookProtected: LiveData<Boolean> = _isNotebookProtected

    private val _noteFile = MutableLiveData<Uri?>()
    val noteFile: LiveData<Uri?> = _noteFile

    private val _noteMoved = MutableLiveData<Boolean>()
    val noteMoved: LiveData<Boolean> = _noteMoved

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _isDateUpdating = MutableStateFlow(false)

    private var pendingSaveContent: String? = null

    init {
        loadNote()
    }

    private fun loadNote() {
        if (noteId != null) viewModelScope.launch {
            _noteEditState.postValue(NoteEditState.Loading)
            try {
                val note = getNoteUseCase(noteId, notebookPath)

                _note.postValue(note)

                if (note.notebookPath != null) viewModelScope.launch {
                    _isNotebookProtected.postValue(isNotebookProtectedUseCase(note.notebookPath))
                }

                val scrollPosition = getNoteScrollPosition()

                _noteEditState.postValue(
                    NoteEditState.Success(
                        note,
                        scrollPosition = scrollPosition,
                        isNotebookProtected.value == true
                    )
                )

                if (note.isNotEmpty()) removeRecentNoteUseCase(note)

            } catch (e: AuthenticationRequiredException) {
                _noteEditState.postValue (NoteEditState.Blocked)
                println("DEBUG: NoteEditVM: Key not unlocked while loading note: ${e.message}")

            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error loading: ${e.message}")
                _noteEditState.postValue (NoteEditState.Error(e.message ?: "Ошибка загрузки"))
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
                //val clearTitle = sanitizeFileName(newTitle)

                if (newTitle != currentNote.title) {
                    _note.postValue(renameNoteUseCase(currentNote, newTitle))
                }
            } catch (e: Exception) {
                showMessage("Ошибка при переименовании заметки: ${e.message}")
            }
        }

    }

    fun updateNoteContent(content: String) {
        //println("DEBUG: NoteEditVM: Updating note content, content =$content")
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

    fun updateFullNote(newTitle: String, content: String) { ///
        val currentNote = _note.value ?: return
        showMessage("Сохранение заметки")
        println("DEBUG: NoteEditVM: Updating full note title=$newTitle, content=${content.take(10)}")

        viewModelScope.launch {
            try {
                val newNote = updateFullNoteUseCase(currentNote, newTitle, content)
                _note.postValue(newNote)
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.postValue (NoteEditState.Blocked)
                println("DEBUG: NoteEditVM: Authentication required while updating full note: ${e.message}")
            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error updating full note: ${e.message}")
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

    fun moveNote(notebookTitle: String) { ///
        val currentNote = _note.value ?: return

        viewModelScope.launch {
            try {
                moveNoteUseCase(currentNote, notebookTitle)
                _noteMoved.postValue(true)
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.value = NoteEditState.Blocked
                println("DEBUG: NoteEditVM: key not unlocked while move note: ${e.message}")
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
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.value = NoteEditState.Blocked
                println("DEBUG: NoteEditVM: key not unlocked while deleting note: ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun saveNoteScrollPosition(scrollPosition: Int) {
        if (noteId != null && notebookPath != null)
            settingsInteractor.saveNoteScrollPosition(noteId, notebookPath, scrollPosition)
    }

    fun getNoteScrollPosition(): Int {
        return if (noteId != null && notebookPath != null) {
            settingsInteractor.getNoteScrollPosition(noteId, notebookPath) ?: 0
        } else 0
    }


    fun unlockNotebook(context: Context) {
        viewModelScope.launch {
            val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                notebookPath,
                context,
                reason = "Для редактирования"
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

    fun refreshNote() = loadNote()

    private fun showMessage(msg: String) = _message.postValue(msg)

    fun clearMessage() = _message.postValue(null)
}

