package ru.whiteleaf.notes.presentation.note_list

import android.content.SharedPreferences
import androidx.core.content.edit
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
import ru.whiteleaf.notes.data.config.NotebookConfigManager
import ru.whiteleaf.notes.data.datasource.EncryptionManager
import ru.whiteleaf.notes.domain.model.BiometricRequest
import ru.whiteleaf.notes.domain.use_case.DecryptExistingNotes
import ru.whiteleaf.notes.domain.use_case.ReEncryptExistingNotes
import ru.whiteleaf.notes.presentation.state.NavigationEvent
import ru.whiteleaf.notes.presentation.state.NoteListState
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
    private val preferences: SharedPreferences,
    private val notebookPath: String?,
    private val configManager: NotebookConfigManager,
    private val encryptionManager: EncryptionManager,
    private val reEncryptExistingNotes: ReEncryptExistingNotes,
    private val decryptExistingNotes: DecryptExistingNotes

) : ViewModel() {

    private val _noteListState = MutableLiveData<NoteListState>()
    val noteListState: LiveData<NoteListState> = _noteListState

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _biometricRequest = MutableLiveData<BiometricRequest?>(null)
    val biometricRequest: LiveData<BiometricRequest?> = _biometricRequest

    private val isProtected = configManager.isNotebookProtected(notebookPath ?: "")
    private val keyAlias = configManager.getKeyAliasForNotebook(notebookPath ?: "")

    init {
        loadNotes()
        saveLastOpenNotebook()
    }

    fun loadNotes() {
        _noteListState.postValue(NoteListState.Loading)

        viewModelScope.launch {
            try {
                val notesList = getNotesUseCase(notebookPath) //если защищенная то будет секьюрити эксепшн

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        showMessage("Пустая заметка удалена")
                    }
                }
                _noteListState.postValue(NoteListState.Success(notesList.filter { it.isNotEmpty() }))

            } catch (e: SecurityException) {
                if (isProtected) {
                    handleBiometricRequired()
                } else {
                    // Неожиданная ошибка - книжка не защищена, но запросила биометрию
                    _noteListState.postValue(NoteListState.Error("Ошибка безопасности"))
                }
            } catch (e: IOException) {
                _noteListState.postValue(NoteListState.Error("Ошибка загрузки заметок: ${e.message}"))
            } catch (e: Exception) {
                _noteListState.postValue(NoteListState.Error("Неизвестная ошибка: ${e.message}"))
            }
        }
    }

//    fun onBiometricSuccess() {
//        _biometricRequest.postValue(null)
//    }

//    fun onBiometricError() {
//        _biometricRequest.postValue(null)
//        showMessage("Аутентификация отменена")
//    }

    fun clearMessage() = showMessage(null)

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

    fun protectNotebook(notebookPath: String) {
        viewModelScope.launch {
            try {
                // Генерируем уникальный ключ для книжки
                val keyAlias = configManager.generateKeyAlias(notebookPath)

                // Создаем ключ в Keystore
                val keyCreated = encryptionManager.getOrCreateKey(keyAlias)

                // Сохраняем конфигурацию
                configManager.setNotebookProtected(notebookPath, keyAlias)

                // Шифруем существующие заметки в этой книжке
                reEncryptExistingNotes(notebookPath)
                showMessage("Записная книжка защищена")

            } catch (e: Exception) {
                showMessage("Ошибка защиты: ${e.message}")
            }
        }
    }

    fun unprotectNotebook(notebookPath: String) {
        viewModelScope.launch {
            try {
                val keyAlias = configManager.getKeyAliasForNotebook(notebookPath)
                if (keyAlias != null) {
                    decryptExistingNotes(notebookPath)
                    encryptionManager.deleteKey(keyAlias)
                }
                configManager.setNotebookUnprotected(notebookPath)
                showMessage("Защита снята")

            } catch (e: Exception) {
                showMessage("Ошибка снятия защиты: ${e.message}")
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

    fun onNoteClicked(noteId: String) = _navigationEvent.postValue(NavigationEvent.NavigateToNote(noteId))

    fun onNavigated() = _navigationEvent.postValue(NavigationEvent.Idle)


    private fun handleBiometricRequired() {
        // Проверяем что книжка действительно защищена
        if (!isProtected) {
            showMessage("Ошибка: книжка не защищена")
            return
        }

        val keyAlias = keyAlias
        if (keyAlias == null) {
            showMessage("Ошибка: ключ шифрования не найден")
            return
        }

        try {
            val cipher = encryptionManager.getCipherForDecryption(keyAlias)

            _biometricRequest.value = BiometricRequest(
                notebookPath = notebookPath,
                keyAlias = keyAlias,
                cipher = cipher,
//                onSuccess = { loadNotes() },
//                onError = { showMessage("Аутентификация не пройдена") }
            )
        } catch (e: Exception) {
            showMessage("Ошибка безопасности: ${e.message}")
        }
    }


    private fun showMessage(s: String?) = _message.postValue(s)

    private fun saveLastOpenNotebook() {
        preferences.edit {
            putString("last_notebook_path", notebookPath)
        }
    }

    fun reloadNotes() {
        loadNotes()
    }
}