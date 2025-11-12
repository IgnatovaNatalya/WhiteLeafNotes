package ru.whiteleaf.notes.presentation.note_list

import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.ShareNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.RenameNotebookByPathUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.repository.SecurityPreferences
import ru.whiteleaf.notes.domain.use_case.CheckNotebookAccessUseCase
import ru.whiteleaf.notes.domain.use_case.ClearNotebookKeysUseCase
import ru.whiteleaf.notes.domain.use_case.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.LockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.UnlockNotebookUseCase
import java.io.IOException

class NoteListViewModel(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val renameNotebookUseCase: RenameNotebookByPathUseCase,
    private val shareNotebookUseCase: ShareNotebookUseCase,
    private val deleteNotebookUseCase: DeleteNotebookByPathUseCase,
    private val encryptNotebookUseCase: EncryptNotebookUseCase,
    private val clearNotebookKeys: ClearNotebookKeysUseCase,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val checkNotebookAccessUseCase: CheckNotebookAccessUseCase,
    private val lockNotebookUseCase: LockNotebookUseCase,
    private val securityPreferences: SecurityPreferences,
    private val preferences: SharedPreferences,
    private val notebookPath: String?
) : ViewModel() {

    private val _noteListState = MutableLiveData<NoteListState>()
    val noteListState: LiveData<NoteListState> = _noteListState

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    // Новые LiveData для безопасности
    private val _notebookSecurityState = MutableLiveData<NotebookSecurityState>()

    private val _authenticationRequired = MutableLiveData<Boolean>()
    val authenticationRequired: LiveData<Boolean> = _authenticationRequired

    private val _encryptionResult = MutableLiveData<Result<Unit>>()
    val encryptionResult: LiveData<Result<Unit>> = _encryptionResult

    private var isEncrypted =
        notebookPath?.let { checkNotebookAccessUseCase.isNotebookEncrypted(it) } == true
    private var hasAccess = true

    init {
        loadNotes()
        saveLastOpenNotebook()
        checkSecurityState()
    }

    private fun checkSecurityState() {

        viewModelScope.launch {
            isEncrypted =
                notebookPath?.let { checkNotebookAccessUseCase.isNotebookEncrypted(it) } == true
            hasAccess = notebookPath?.let { checkNotebookAccessUseCase(it) } != false

            _notebookSecurityState.postValue(///
                NotebookSecurityState(
                    isEncrypted = isEncrypted,
                    isUnlocked = hasAccess,
                    requiresAuthentication = isEncrypted && !hasAccess
                )
            )
            _authenticationRequired.postValue(isEncrypted && !hasAccess)///
        }
    }

    fun loadNotes() {
        _noteListState.postValue(NoteListState.Loading)

        viewModelScope.launch {
            if (notebookPath != null) {
                val hasAccess = checkNotebookAccessUseCase(notebookPath)
                if (!hasAccess) {
                    _noteListState.postValue(NoteListState.Blocked)
                    return@launch
                }
            }
            try {
                val notesList = getNotesUseCase(notebookPath)

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        _message.postValue("Пустая заметка удалена")
                    }
                }
                _noteListState.postValue(
                    NoteListState.Success(
                        isEncrypted,
                        notesList.filter { it.isNotEmpty() })
                )

            } catch (e: IOException) {
                _noteListState.postValue(NoteListState.Error("Ошибка загрузки заметок: ${e.message}"))
            } catch (e: Exception) {
                showMessage("Неизвестная ошибка: ${e.message}")
                _noteListState.postValue(NoteListState.Error("Неизвестная ошибка: ${e.message}"))
            } finally {
                println("Окончание загрузки заметок")
            }
        }
    }

    fun encryptNotebook() {
        viewModelScope.launch {
            if (notebookPath != null) {
                // Проверяем, не зашифрован ли уже блокнот
                if (securityPreferences.isNotebookEncrypted(notebookPath)) {
                    showMessage("Блокнот уже зашифрован")
                    return@launch
                }

                encryptNotebookUseCase(notebookPath)
                    .onSuccess {
                        showMessage("Блокнот успешно зашифрован")
                        checkSecurityState()
                        loadNotes() // Перезагружаем чтобы показать заблокированное состояние
                    }
                    .onFailure { error ->
                        val errorMessage = error.message ?: "Неизвестная ошибка"
                        showMessage("Ошибка шифрования: $errorMessage")
                        println("❌ Ошибка в ViewModel: $errorMessage")
                        error.printStackTrace()
                    }
            }
        }
    }

    fun unlockNotebook(activity: FragmentActivity) {
        viewModelScope.launch {
            if (notebookPath != null) {

                unlockNotebookUseCase(notebookPath, activity).onSuccess {
                    showMessage("Блокнот разблокирован")
                    checkSecurityState()
                    loadNotes() // Загружаем расшифрованные заметки
                }.onFailure { error ->
                    showMessage(error.message.toString())
                    println("❌ Ошибка разблокировки: ${error.message}")
                    error.printStackTrace()
                }
            }
        }
    }

    fun lockNotebook() {
        showMessage("Шифруем")
        if (notebookPath != null) {
            lockNotebookUseCase(notebookPath)
            showMessage("Записная книжка зашифрована")
            checkSecurityState()
            loadNotes() // Обновляем список заметок (должен стать пустым)
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

    fun renameNotebook(newName: String) {
        viewModelScope.launch {
            try {
                if (newName != notebookPath && notebookPath != null) {
                    renameNotebookUseCase(notebookPath, newName)
                    showMessage("Название записной книжки изменено")
                    _navigationEvent.postValue(NavigationEvent.NavigateToNotebook(newName))
                } else showMessage("Ошибка переименования")
            } catch (e: Exception) {
                showMessage("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun shareNotebook() {
        viewModelScope.launch {
            if (notebookPath != null)
                try {
                    val result = shareNotebookUseCase(notebookPath)

                    if (result.isSuccess)
                        _navigationEvent.postValue(NavigationEvent.ExportLink(result.getOrNull()))
                    else
                        showMessage(result.exceptionOrNull()?.message ?: "Unknown error")

                } catch (e: Exception) {
                    showMessage("Ошибка передачи файла записной книжки: ${e.message}")
                }
        }
    }

    fun deleteNotebook() {
        viewModelScope.launch {
            try {
                if (notebookPath != null) {
//                    println("🗑️ НАЧАЛО УДАЛЕНИЯ БЛОКНОТА: $notebookPath")
//                    println("🔑 Очищаем ключи...")
//                    clearNotebookKeys(notebookPath)
//
//                    println("📊 Сбрасываем состояние безопасности...")
//                    securityPreferences.setNotebookEncrypted(notebookPath, false)
//                    securityPreferences.setNotebookUnlocked(notebookPath, false)
//
//                    println("📁 Удаляем файлы блокнота...")

                    deleteNotebookUseCase(notebookPath)
                    //println("✅ Блокнот полностью удален")
                    _navigationEvent.postValue(NavigationEvent.NavigateUp)
                    showMessage("Записная книжка удалена")

                } else showMessage("Ошибка удаления записной книжки: путь не задан")
            } catch (e: Exception) {
                showMessage("Ошибка удаления записной книжки: ${e.message}")
            }
        }
    }

    fun onNoteClicked(noteId: String) =
        _navigationEvent.postValue(NavigationEvent.NavigateToNote(noteId))

    fun onNavigated() = _navigationEvent.postValue(NavigationEvent.Idle)


    fun clearMessage() = _message.postValue(null)

    private fun saveLastOpenNotebook() {
        preferences.edit {
            putString("last_notebook_path", notebookPath)
        }
    }



    fun onNotebookExited(toNote:Boolean) {
        if (toNote) return
        if (notebookPath != null) {
            println("🔒 Блокируем блокнот при выходе: $notebookPath")
            lockNotebookUseCase(notebookPath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        onNotebookExited(false)
    }
}