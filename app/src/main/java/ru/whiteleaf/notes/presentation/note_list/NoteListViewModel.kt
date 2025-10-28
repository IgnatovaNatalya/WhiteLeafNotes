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
import ru.whiteleaf.notes.presentation.state.ExportState
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.data.config.NotebookConfigManager
import ru.whiteleaf.notes.data.datasource.EncryptionManager
import ru.whiteleaf.notes.domain.model.BiometricRequest
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
    private val encryptionManager: EncryptionManager
) : ViewModel() {

    private val _notes = MutableLiveData<List<Note>>()
    val notes: LiveData<List<Note>> = _notes

    private val _shareNotebookState = MutableLiveData<ExportState>()
    val shareNotebookState: LiveData<ExportState> = _shareNotebookState



    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _navigateToNote = MutableLiveData<String?>()
    val navigateToNote: LiveData<String?> = _navigateToNote

    private val _biometricRequest = MutableLiveData<BiometricRequest?>(null)
    val biometricRequest: LiveData<BiometricRequest?> = _biometricRequest

    private val _navigateToCreatedNote = MutableLiveData<String?>()
    val navigateToCreatedNote: LiveData<String?> = _navigateToCreatedNote

    private val _navigateToRenamed = MutableLiveData<String>()
    val navigateToRenamed: LiveData<String> = _navigateToRenamed

    private val _navigateUpAfterDelete = MutableLiveData<Boolean>()
    val navigateUpAfterDelete: LiveData<Boolean> = _navigateUpAfterDelete

    private val isProtected = configManager.isNotebookProtected(notebookPath ?: "")
    private val keyAlias = configManager.getKeyAliasForNotebook(notebookPath ?: "")



    init {
        loadNotes()
        saveLastOpenNotebook()
    }

    fun loadNotes() {
        _isLoading.postValue(true)
        showMessage(null)

        viewModelScope.launch {
            try {
                val notesList = getNotesUseCase(notebookPath)

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        showMessage("Пустая заметка удалена")
                    }
                }
                _notes.value = notesList.filter { it.isNotEmpty() }

            } catch (e: SecurityException) {
                if (isProtected) {
                    handleBiometricRequired()
                } else {
                    // Неожиданная ошибка - книжка не защищена, но запросила биометрию
                    showMessage("Ошибка безопасности")
                }
            } catch (e: IOException) {
                showMessage("Ошибка загрузки заметок: ${e.message}")
                _notes.postValue(emptyList())
            } catch (e: Exception) {
                showMessage("Неизвестная ошибка: ${e.message}")
                _notes.postValue(emptyList())
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun onBiometricSuccess() {
        _biometricRequest.postValue(null)
    }

    fun onBiometricError() {
        _biometricRequest.postValue(null)
        showMessage("Аутентификация отменена")
    }

    fun clearMessage() = showMessage(null)

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(notebookPath)
                _navigateToCreatedNote.postValue(newNote.id)
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
                    reloadNotes()
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
                    _navigateToRenamed.postValue(newName)
                } else showMessage("Ошибка переименования")
            } catch (e: Exception) {
                showMessage("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun protectNotebook(notebookPath: String) {
        viewModelScope.launch {
            _protectionState.value = ProtectionState.Protecting(notebookPath)

            try {
                // Генерируем уникальный ключ для книжки
                val keyAlias = configManager.generateKeyAlias(notebookPath)

                // Создаем ключ в Keystore
                val keyCreated = encryptionManager.createKeyForNotebook(keyAlias)
                if (!keyCreated) {
                    _protectionState.value = ProtectionState.Error("Не удалось создать ключ безопасности")
                    return@launch
                }

                // Сохраняем конфигурацию
                configManager.setNotebookProtected(notebookPath, keyAlias)

                // Шифруем существующие заметки в этой книжке
                reencryptExistingNotes(notebookPath, keyAlias)

                _protectionState.value = ProtectionState.Success("Записная книжка защищена")

            } catch (e: Exception) {
                _protectionState.value = ProtectionState.Error("Ошибка защиты: ${e.message}")
            } finally {
                // Автоматически сбрасываем состояние через 3 секунды
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _protectionState.value = null
                }
            }
        }
    }

    fun unprotectNotebook(notebookPath: String) {
        viewModelScope.launch {
            _protectionState.value = ProtectionState.Unprotecting(notebookPath)

            try {
                val keyAlias = configManager.getKeyAliasForNotebook(notebookPath)
                if (keyAlias != null) {
                    // Расшифровываем заметки
                    decryptExistingNotes(notebookPath, keyAlias)
                    // Удаляем ключ
                    encryptionManager.deleteKey(keyAlias)
                }

                // Удаляем из конфигурации
                configManager.setNotebookUnprotected(notebookPath)

                _protectionState.value = ProtectionState.Success("Защита снята")

            } catch (e: Exception) {
                _protectionState.value = ProtectionState.Error("Ошибка снятия защиты: ${e.message}")
            } finally {
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _protectionState.value = null
                }
            }
        }
    }

    fun shareNotebook() {
        _shareNotebookState.postValue(ExportState.Loading)

        viewModelScope.launch {
            if (notebookPath != null)
                try {
                    val result = shareNotebookUseCase(notebookPath)
                    if (result.isSuccess)
                        _shareNotebookState.postValue(ExportState.Success(result.getOrNull()))
                    else
                        _shareNotebookState.postValue(
                            ExportState.Error(
                                result.exceptionOrNull()?.message ?: "Unknown error"
                            )
                        )

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
                    _navigateUpAfterDelete.postValue(true)
                    showMessage("Записная книжка удалена")
                } else showMessage("Ошибка удаления записной книжки: путь не задан")
            } catch (e: Exception) {
                showMessage("Ошибка удаления записной книжки: ${e.message}")
            }
        }
    }

    fun onNoteClicked(noteId: String) = _navigateToNote.postValue(noteId)

    fun onNoteNavigated() = _navigateToNote.postValue(null)

    fun onNoteCreatedNavigated() = _navigateToCreatedNote.postValue(null)


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
                onSuccess = { loadNotes() },
                onError = { showMessage("Аутентификация не пройдена") }
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