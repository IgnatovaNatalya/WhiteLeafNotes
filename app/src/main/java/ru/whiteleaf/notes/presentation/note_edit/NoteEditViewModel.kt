package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
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
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase

class NoteEditViewModel(
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val saveNoteContentUseCase: SaveNoteContentUseCase,
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

    private val _navigationEvent = MutableLiveData<NoteEditNavigationEvent?>()
    val navigationEvent: LiveData<NoteEditNavigationEvent?> = _navigationEvent

    private var currentNote: Note? = null
    fun getNote() = currentNote

    private val _isDateUpdating = MutableStateFlow(false)
    private var pendingSaveContent: String? = null
    private var currentScrollPosition: Int? = null
    private var notebookList: List<Notebook> = emptyList()

    init {
        viewModelScope.launch { loadNote() }
        loadNotebooks()
    }

    fun getEncryptionStatus(): Boolean {
        return isNotebookProtectedUseCase(notebookPath ?: "")
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
        val note = currentNote ?: return
        if (_noteEditState.value is NoteEditState.Success) postNote(note)
    }

    private suspend fun loadNote() {
        if (noteId != null) {//viewModelScope.launch {
            _noteEditState.postValue(NoteEditState.Loading)
            println("DEBUG: NoteEditVM: Loading note id=$noteId path=$notebookPath")
            try {
                val note = getNoteUseCase(noteId, notebookPath)
                currentNote = note
                if (currentScrollPosition == null) currentScrollPosition = getNoteScrollPosition()
                println("DEBUG: NoteEditVM: Note loaded: ${note.printDebug()}. scroll=$currentScrollPosition")

                postNote(note)

                if (note.isNotEmpty()) removeRecentNoteUseCase(note)

            } catch (e: AuthenticationRequiredException) {
                _navigationEvent.postValue(NoteEditNavigationEvent.ShowBiometric)
                println("DEBUG: NoteEditVM: Key not unlocked while loading note: ${e.message}")

            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error loading: ${e.message}")
                _noteEditState.postValue(NoteEditState.Error(e.message ?: "Ошибка загрузки"))
            }
        }
    }

    fun lockNote() {
        _noteEditState.value = NoteEditState.Blocked(false)
    }

    fun updateNoteTitleIfChanged(newTitle: String) {
        val note = currentNote ?: return
        if (newTitle == note.title) return

        viewModelScope.launch {
            try {
                println("DEBUG: NoteEditVM: Updating note title to $newTitle, currentNote: ${currentNote?.printDebug()}")
                currentNote = renameNoteUseCase(note, newTitle)
                postNote(currentNote!!)
                //reopenNote(currentNote?.id ?: "")
            } catch (e: Exception) {
                postNote(note)  //если название изменить не удалось, возвращаем прежнее
                showMessage("Ошибка при переименовании заметки: ${e.message}")
            }
        }
    }

    fun updateNoteContent(content: String) {
        println("DEBUG: NoteEditVM: Updating note content, current note: ${currentNote?.printDebug()}")
        val note = currentNote ?: return
        viewModelScope.launch {
            try {
                saveNoteContentUseCase(note.copy(content = content))
                currentNote = note.copy(content = content)
            } catch (e: AuthenticationRequiredException) {
                pendingSaveContent = content
                _noteEditState.postValue(NoteEditState.Blocked(false))
                println("DEBUG: NoteEditVM: key not unlocked while updating: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка при сохранении текста заметки: ${e.message}")
                println("DEBUG: NoteEditVM: error: ${e.message}")
            }
        }
    }

    fun unlockNote(context: Context) {
        if (currentNote == null)
            unlockAndLoad(context)
        else
            unlockAndSavePendingContent(context)
    }

    fun unlockAndLoad(context: Context) {
        viewModelScope.launch {
            val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                notebookPath, context, title = "Заметка защищена", reason = "Для разблокирования"
            ) else true

            if (unlocked) loadNote()
            else _noteEditState.postValue(NoteEditState.Blocked(false))
        }
    }

    fun unlockAndSavePendingContent(context: Context) {
        val note = currentNote ?: return
        println("DEBUG: NoteEditVM: unlockAndSavePendingContent: ${currentNote?.printDebug()}")

        viewModelScope.launch {
            try {
                val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                    notebookPath, context, reason = "Для редактирования"
                ) else true

                if (unlocked) {
                    val newContent = pendingSaveContent

                    if (newContent != null) {
                        val updatedNote = note.copy(content = newContent)
                        saveNoteContentUseCase(updatedNote)
                        postNote(updatedNote)
                        currentNote = updatedNote
                        pendingSaveContent = null
                    } else {
                        postNote(note)
                    }
                } else {
                    _noteEditState.postValue(NoteEditState.Blocked(true))
                }
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.postValue(NoteEditState.Blocked(true))
                println("DEBUG: NoteEditVM: key not unlocked while updating: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка при сохранении текста заметки: ${e.message}")
                println("DEBUG: NoteEditVM: unlockAndSavePendingContent: error: ${e.message}")
            }
        }
    }

    fun updateNoteDate(newDate: Long) {
        val note = currentNote ?: return

        viewModelScope.launch {
            _isDateUpdating.value = true

            try {
                updateNoteDateUseCase(note, newDate)

                currentNote = note.copy(modifiedAt = newDate)
                _noteEditState.postValue(
                    NoteEditState.Success(
                        note.copy(modifiedAt = newDate),
                        currentScrollPosition ?: 0,
                        isNotebookProtectedUseCase(note.notebookPath ?: "")
                    )
                )
                showMessage("Дата заметки обновлена")
            } catch (e: Exception) {
                showMessage("Ошибка обновления даты: ${e.message}")
            } finally {
                _isDateUpdating.value = false
            }
        }
    }

    private fun postNote(note: Note) {
        _noteEditState.postValue(
            NoteEditState.Success(
                note,
                currentScrollPosition ?: 0,
                if (!note.notebookPath.isNullOrBlank()) isNotebookProtectedUseCase(note.notebookPath) else true
            )
        )
    }

    private fun navigateBack() =
        _navigationEvent.postValue(NoteEditNavigationEvent.NavigateBack)

    fun saveToRecent() {
        val note = currentNote ?: return
        viewModelScope.launch {
            try {
                saveRecentNoteUseCase(note)
            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error saving note to recent: ${e.message}")
            }
        }
    }

    fun shareFile(context: Context) {
        val note = currentNote ?: return
        viewModelScope.launch {
            try {
                val unlocked = if (notebookPath != null)
                    unlockNotebookUseCase(notebookPath, context, reason = "Для экспорта") else true

                val file = shareNoteFileUseCase(note)

                if (unlocked)
                    _navigationEvent.postValue(
                        NoteEditNavigationEvent.ShareFile(file)
                    )
                else _noteEditState.postValue(NoteEditState.Blocked(false))

            } catch (e: Exception) {
                showMessage("Ошибка при экспорте заметки: ${e.message}")
            }
        }
    }

    fun moveNote(context: Context, targetNotebookPath: String) {
        val note = currentNote ?: return

        viewModelScope.launch {
            try {
                val unlocked =
                    if (isNotebookProtectedUseCase(targetNotebookPath)) unlockNotebookUseCase(
                        targetNotebookPath, context, title = "Целевая записная книжка защищена",
                        reason = "Для перемещения"
                    ) else true

                if (unlocked) {
                    moveNoteUseCase(note, targetNotebookPath)
                    navigateBack()
                } else {
                    showMessage("Не удалось разблокировать целевую записную книжку")
                }
            } catch (e: AuthenticationRequiredException) {
                showMessage("Не удалось разблокировать целевую записную книжку")
                println("DEBUG: NoteEditVM:  AuthenticationRequiredException ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка перемещения: ${e.message}")
            }
        }
    }

    fun deleteNote() {
        val note = currentNote ?: return

        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                navigateBack()
            } catch (e: AuthenticationRequiredException) { //не может такого быть
                println("DEBUG: NoteEditVM: key not unlocked while deleting note: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun saveNoteScrollPosition(scrollPosition: Int) {
        if (noteId != null) {
            println("DEBUG: NoteEditVM: saveNoteScrollPosition: noteId=$noteId, notebookPath=$notebookPath, pos=$scrollPosition")
            settingsInteractor.saveNoteScrollPosition(noteId, notebookPath ?: "", scrollPosition)
        }
    }

    fun rememberNoteScrollPosition(scrollPosition: Int) {
        println("DEBUG: NoteEditVM: rememberNoteScrollPosition: noteId=$noteId, notebookPath=$notebookPath, pos=$scrollPosition")
        currentScrollPosition = scrollPosition
    }

    fun getNoteScrollPosition(): Int {
        return if (noteId != null) {
            settingsInteractor.getNoteScrollPosition(noteId, notebookPath ?: "") ?: 0
        } else 0
    }

    private fun showMessage(msg: String) =
        _navigationEvent.postValue(NoteEditNavigationEvent.ShowMessage(msg))

    fun clearEvent() {
        _navigationEvent.value = null
    }
}

