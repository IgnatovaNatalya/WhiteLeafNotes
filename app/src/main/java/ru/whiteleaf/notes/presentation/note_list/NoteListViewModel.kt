package ru.whiteleaf.notes.presentation.note_list

import android.content.Context
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
import ru.whiteleaf.notes.domain.use_case.RenameNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.domain.repository.EncryptionRepository
import ru.whiteleaf.notes.domain.interactor.PreferencesInteractor
import ru.whiteleaf.notes.domain.repository.KeyNotUnlockedException
import ru.whiteleaf.notes.domain.use_case.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.EncryptNotebookUseCase
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
    private val encryptionRepository: EncryptionRepository,
    private val encryptNotebookUseCase: EncryptNotebookUseCase,
    private val decryptNotebookUseCase: DecryptNotebookUseCase,
    private val preferencesInteractor: PreferencesInteractor,
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

                    isProtected = encryptionRepository.hasKey(notebookPath)
                    val isUnlocked = encryptionRepository.isUnlocked(notebookPath)

                    if (isProtected && !isUnlocked) {
                        _noteListState.postValue(NoteListState.Blocked)
                        return@launch
                    }
                }

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

            } catch (e: KeyNotUnlockedException) {
                _noteListState.postValue(NoteListState.Blocked)
            } catch (e: IOException) {
                _noteListState.postValue(NoteListState.Error("Ошибка загрузки заметок: ${e.message}"))
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteListState.postValue(NoteListState.Blocked)
                    if (notebookPath != null) encryptionRepository.lockNotebook(notebookPath)
                } else _noteListState.postValue(NoteListState.Error(e.message ?: "Ошибка загрузки"))
            } finally {
                println("DEBUG: NoteListViewmodel: Окончание загрузки заметок")
            }
        }
    }

    fun unlockNotebook(context: Context) {
        val isProtected =
            if (notebookPath != null) encryptionRepository.hasKey(notebookPath) else false
        println("DEBUG: unlock notebook, notebookPath = $notebookPath, isProtected = $isProtected")

        if (notebookPath != null && isProtected)
            viewModelScope.launch {
                val unlocked = encryptionRepository.unlockNotebook(notebookPath, context)
                if (unlocked) {
                    println("DEBUG: unlock notebook: success")
                    loadNotes()
                } else {
                    _noteListState.postValue(NoteListState.Blocked)
                    //   _noteListState.postValue(NoteListState.Error("Не удалось разблокировать записную книжку"))
                }
            }
    }

    fun lockNotebook() {
        notebookPath?.let { encryptionRepository.lockNotebook(it) }
    }

    fun encryptNotebook(context: Context) {
        if (notebookPath != null) viewModelScope.launch {
            try {
                if (encryptionRepository.hasKey(notebookPath)) {
                    _noteListState.value = NoteListState.Error("Блокнот уже защищён")
                    return@launch
                }

                encryptionRepository.createKeyForNotebook(notebookPath)
                encryptNotebookUseCase(notebookPath)
                loadNotes()
                _message.value = "Записная книжка зашифрована"

            } catch (e: Exception) {
                _noteListState.value = NoteListState.Error("Ошибка шифрования: ${e.message}")
                println("DEBUG: Ошибка шифрования: ${e.message}")
            }
        }
    }

    fun decryptNotebook(context: Context) {
        if (notebookPath != null) viewModelScope.launch {
            try {
                if (!encryptionRepository.hasKey(notebookPath)) {
                    _noteListState.value = NoteListState.Error("Блокнот не защищён")
                    return@launch
                }
                // Биометрия нужна для чтения зашифрованных файлов
                val unlocked = encryptionRepository.unlockNotebook(notebookPath, context)
                if (!unlocked) {
                    _noteListState.value =
                        NoteListState.Error("Не удалось подтвердить личность")
                    return@launch
                }
                // Расшифровываем все заметки
                decryptNotebookUseCase(notebookPath)
                // Удаляем ключ
                encryptionRepository.deleteKeyForNotebook(notebookPath)
                // Перезагружаем список (теперь файлы в открытом виде)
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
                    deleteNotebookUseCase(notebookPath)
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

    private fun saveLastOpenedNotebook() {
        notebookPath?.let { preferencesInteractor.saveLastOpenedNotebook(notebookPath) }
    }

    fun onNotebookExited(toNote: Boolean) {
        if (toNote) return
        if (notebookPath != null) {
            println("🔒 Блокируем записную книжку при выходе: $notebookPath")
            encryptionRepository.lockNotebook(notebookPath)
        }
    }

    override fun onCleared() {
        super.onCleared()
        onNotebookExited(false)
    }
}