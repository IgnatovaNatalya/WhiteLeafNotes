package ru.whiteleaf.notes.presentation.note_list

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.interactor.SettingsInteractor
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookUnlockedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.LockNotebookUseCase
import java.io.IOException
import java.security.InvalidKeyException

class NoteListViewModel(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val shareNotebookUseCase: ShareNotebookUseCase,
    private val deleteNotebookUseCase: DeleteNotebookByPathUseCase,
    private val isNotebookProtectedUseCase: IsNotebookProtectedUseCase,
    private val isNotebookUnlockedUseCase: IsNotebookUnlockedUseCase,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val lockNotebookUseCase: LockNotebookUseCase,
    private val createKeyForNotebookUseCase: CreateKeyForNotebookUseCase,
    private val deleteKeyForNotebookUseCase: DeleteKeyForNotebookUseCase,
    private val encryptNotebookUseCase: EncryptNotebookUseCase,
    private val decryptNotebookUseCase: DecryptNotebookUseCase,
    private val preferencesInteractor: SettingsInteractor,
    private val notebookPath: String?
) : ViewModel() {

    private val _noteListState = MutableLiveData<NoteListState>()
    val noteListState: LiveData<NoteListState> = _noteListState

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    private val _isPlannerView = MutableLiveData(false)

    init {
        loadViewMode()
        loadNotes()
        saveLastOpenedNotebook()
    }

    private fun loadViewMode() {
        if (notebookPath == null) return
        val savedMode = preferencesInteractor.getViewMode(notebookPath)
        _isPlannerView.value = savedMode
    }

    fun setViewMode(isPlanner: Boolean) {
        if (notebookPath == null) return
        _isPlannerView.value = isPlanner
        preferencesInteractor.saveViewMode(notebookPath, isPlanner)
        _navigationEvent.postValue(NavigationEvent.NavigateToNotebook(notebookPath))
    }

    fun getViewMode(): Boolean {
        return _isPlannerView.value ?: false
    }

    fun loadNotes() {
        viewModelScope.launch {
            _noteListState.postValue(NoteListState.Loading)
            var isProtected = false

            try {
                if (notebookPath != null) {

                    isProtected = isNotebookProtectedUseCase(notebookPath)
                    val isUnlocked = isNotebookUnlockedUseCase(notebookPath)

                    if (isProtected && !isUnlocked) {
                        _noteListState.postValue(NoteListState.Blocked)
                        _navigationEvent.postValue(NavigationEvent.ShowBiometric)
                        return@launch
                    }
                }

                println("DEBUG: NoteListViewmodel: Загрузка заметок, isProtected= $isProtected")
                val notesList = getNotesUseCase(notebookPath)

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        _message.postValue("Пустая заметка удалена")
                    }
                }
                _noteListState.postValue(
                    NoteListState.Success(isProtected, notesList.filter { it.isNotEmpty() })
                )

            } catch (_: AuthenticationRequiredException) {
                _noteListState.postValue(NoteListState.Blocked)
            } catch (e: IOException) {
                _noteListState.postValue(NoteListState.Error("Ошибка загрузки заметок: ${e.message}"))
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteListState.postValue(NoteListState.Blocked)
                    if (notebookPath != null) lockNotebookUseCase(notebookPath)
                    println("DEBUG: NoteListViewmodel: InvalidKeyException ${e.message}")
                } else
                    _noteListState.postValue(NoteListState.Error(e.message ?: "Ошибка загрузки"))
            } finally {
                println("DEBUG: NoteListViewmodel: Окончание загрузки заметок")
            }
        }
    }

    fun unlockNotebook(context: Context) {
        val isProtected =
            if (notebookPath != null) isNotebookProtectedUseCase(notebookPath) else false //encryptionRepository.hasKey(notebookPath) else false
        println("DEBUG: unlock notebook, notebookPath = $notebookPath, isProtected = $isProtected")

        if (notebookPath != null && isProtected)
            viewModelScope.launch {
                val unlocked = unlockNotebookUseCase(notebookPath, context)
                if (unlocked) {
                    println("DEBUG: unlock notebook: success")
                    loadNotes()
                } else {
                    _noteListState.postValue(NoteListState.Blocked)
                }
            }
    }

    fun lockNotebook() {
        if (notebookPath != null) {
            lockNotebookUseCase(notebookPath)
            _noteListState.postValue(NoteListState.Blocked)
        }
    }

    fun encryptNotebook(context: Context) {
        if (notebookPath != null) viewModelScope.launch {
            try {
                if (isNotebookProtectedUseCase(notebookPath)) {
                    _noteListState.value = NoteListState.Error("Записная книжка уже защищищена")
                    return@launch
                }
                createKeyForNotebookUseCase(notebookPath)
                unlockNotebookUseCase(
                    notebookPath,
                    context,
                    title = "Защита записной кникки",
                    reason = "Для защиты"
                )
                encryptNotebookUseCase(notebookPath)
                loadNotes()
                _message.value = "Записная книжка зашифрована"
            } catch (e: AuthenticationRequiredException) {
                _noteListState.value =
                    NoteListState.Error("Не удалось зашифровать записную книжку: ${e.message}")
                //_noteListState.value = NoteListState.Blocked
                println("DEBUG: NoteListVM fail encrypt notebook: ${e.message}")

            } catch (e: Exception) {
                _noteListState.value = NoteListState.Error("Ошибка шифрования: ${e.message}")
                println("DEBUG: Ошибка шифрования: ${e.message}")
            }
        }
    }

    fun decryptNotebook(context: Context) {
        if (notebookPath != null) viewModelScope.launch {
            try {
                if (!isNotebookProtectedUseCase(notebookPath)) {
                    _noteListState.value = NoteListState.Error("Защита не установлена")
                    return@launch
                }
                val unlocked =
                    unlockNotebookUseCase(notebookPath, context, reason = "Для снятия защиты")
                if (!unlocked) {
                    _noteListState.value =
                        NoteListState.Error("Не удалось подтвердить личность")
                    return@launch
                }
                decryptNotebookUseCase(notebookPath)
                deleteKeyForNotebookUseCase(notebookPath)
                loadNotes()
                _message.value = "Защита записной книжки снята"
            } catch (e: Exception) {
                _noteListState.value = NoteListState.Error("Ошибка расшифровки: ${e.message}")
            }
        }
    }

    private fun showMessage(msg: String) = _message.postValue(msg)

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(notebookPath)
                _navigationEvent.postValue(NavigationEvent.NavigateToNote(newNote.id))
            } catch (e: Exception) {
                showMessage("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                loadNotes()
            } catch (e: Exception) {
                showMessage("Ошибка удаления заметки: ${e.message}")
            }
        }
    }


    fun moveNote(note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                moveNoteUseCase(note, targetNotebookPath)
                loadNotes()
            } catch (e: Exception) {
                showMessage("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNoteUseCase(note, newTitle)
                    loadNotes()
                    showMessage("Название заметки изменено")
                    //reloadNotes()
                }
            } catch (e: Exception) {
                showMessage("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun renameNotebook(newName: String, context: Context) {
        if (notebookPath != null)
            viewModelScope.launch {
                try {
                    val unlocked =
                        unlockNotebookUseCase(notebookPath, context, reason = "Для переименования")

                    if (!unlocked) {
                        _message.postValue("Не удалось подтвердить личность")
                        return@launch
                    }

                    if (newName != notebookPath) {
                        renameNotebookUseCase(notebookPath, newName)
                        showMessage("Название записной книжки изменено")
                        _navigationEvent.postValue(NavigationEvent.NavigateToNotebook(newName))
                    } else showMessage("Ошибка переименования")
                } catch (e: AuthenticationRequiredException) {
                    _message.postValue("Записная книжка заблокирована: ${e.message}")
                } catch (e: Exception) {
                    showMessage("Ошибка переименования: ${e.message}")
                }
            }
    }

    fun shareNotebook(context: Context) {
        viewModelScope.launch {
            if (notebookPath != null)
                try {
                    val unlocked = if (isNotebookProtectedUseCase(notebookPath))
                        unlockNotebookUseCase(notebookPath, context, reason = "Для экспорта")
                    else
                        true

                    if (unlocked) {
                        val result = shareNotebookUseCase(notebookPath, "123") //todo пароль спрашивать

                        if (result.isSuccess)
                            _navigationEvent.postValue(NavigationEvent.ExportLink(result.getOrNull()))
                        else
                            showMessage(result.exceptionOrNull()?.message ?: "Unknown error")
                    } else {
                        _message.postValue("Не удалось подтвердить личность")
                        return@launch
                    }

                } catch (e: AuthenticationRequiredException) {
                    _message.postValue("Записная книжка заблокирована: ${e.message}")
                } catch (e: Exception) {
                    showMessage("Ошибка передачи файла записной книжки: ${e.message}")
                }
        }
    }

    fun deleteNotebook(context: Context) {
        if (notebookPath != null)
            viewModelScope.launch {
                try {
                    if (isNotebookProtectedUseCase(notebookPath)) {
                        val unlocked =
                            unlockNotebookUseCase(notebookPath, context, reason = "Для удаления")

                        if (!unlocked) {
                            _message.postValue("Не удалось подтвердить личность")
                            return@launch
                        }
                    }

                    deleteNotebookUseCase(notebookPath)

                    _navigationEvent.postValue(NavigationEvent.NavigateUp)
                    showMessage("Записная книжка удалена")

                } catch (e: Exception) {
                    showMessage("Ошибка удаления записной книжки: ${e.message}")
                }
            }
    }

    fun onNoteClicked(noteId: String) =
        _navigationEvent.postValue(NavigationEvent.NavigateToNote(noteId))

    fun onNavigated() = _navigationEvent.postValue(NavigationEvent.Idle)

    fun clearMessage() = _message.postValue(null)

    private fun saveLastOpenedNotebook() {
        notebookPath?.let { preferencesInteractor.saveLastOpenedNotebook(notebookPath) }
    }

    fun onNotebookExited(toNote: Boolean) {
        println("DEBUG: Выход из блокнота $notebookPath, toNote=$toNote")
        val state = _noteListState.value

        if (toNote || state !is NoteListState.Success) return
        println("DEBUG: isEncrypted = ${state.isEncrypted} ")

        if (notebookPath != null)
            if (state.isEncrypted) {
                println("DEBUG: убираем признак разблокировки при выходе: $notebookPath")
                lockNotebookUseCase(notebookPath)
            }
    }

    override fun onCleared() {
        super.onCleared()
        onNotebookExited(false)
    }
}