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
import ru.whiteleaf.notes.domain.use_case.GetEncryptedContentSampleUseCase
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
    private val decryptExistingNotes: DecryptExistingNotes,
    private val getEncryptedContentSample: GetEncryptedContentSampleUseCase

) : ViewModel() {

    private val _noteListState = MutableLiveData<NoteListState>()
    val noteListState: LiveData<NoteListState> = _noteListState

    private val _navigationEvent = MutableLiveData<NavigationEvent>()
    val navigationEvent: LiveData<NavigationEvent> = _navigationEvent

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _biometricRequest = MutableLiveData<BiometricRequest>()
    val biometricRequest: LiveData<BiometricRequest> = _biometricRequest

    private var isProtected = configManager.isNotebookProtected(notebookPath ?: "")
    private var keyAlias = configManager.getKeyAliasForNotebook(notebookPath ?: "")

    init {
        loadNotes()
        saveLastOpenNotebook()
    }

    fun loadNotes() {

        println("🔄 ViewModel.loadNotes START - notebook: $notebookPath")

        viewModelScope.launch {
            try {
                _noteListState.postValue(NoteListState.Loading)
                val notesList =
                    getNotesUseCase(notebookPath) //если защищенная то будет секьюрити эксепшн

                println("✅ ViewModel got notes: ${notesList.size}")

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        showMessage("Пустая заметка удалена")
                    }
                }
                isProtected = configManager.isNotebookProtected(notebookPath ?: "")

                _noteListState.postValue(
                    NoteListState.Success(
                        isProtected,
                        notesList.filter { it.isNotEmpty() })
                )

            } catch (e: SecurityException) {
                println("🔐 ViewModel caught SecurityException: ${e.message}")
                //_noteListState.postValue(NoteListState.Blocked)
                //if (isProtected) {
                _noteListState.postValue(NoteListState.Blocked)
                //requestBiometricAuthentication()
                //} else {
                // Неожиданная ошибка - книжка не защищена, но запросила биометрию
                //    _noteListState.postValue(NoteListState.Error("Ошибка безопасности"))
                //}
            } catch (e: IOException) {
                println("❌ ViewModel caught other exception: ${e.message}")
                _noteListState.postValue(NoteListState.Error("Ошибка загрузки заметок: ${e.message}"))
            } catch (e: Exception) {
                _noteListState.postValue(NoteListState.Error("Неизвестная ошибка: ${e.message}"))
            } finally {
                println("🔄 ViewModel.loadNotes END")
            }
        }
    }

    fun requestBiometricAuthentication() {
        viewModelScope.launch {
            println("🔐 ViewModel.requestBiometricAuthentication START")
            try {
                val keyAlias = configManager.getKeyAliasForNotebook(notebookPath ?: "")
                println("🔐 Key alias: $keyAlias")

                if (keyAlias != null) {
                    // Нужно получить зашифрованный текст для создания Cipher с IV

                    val encryptedContent = getEncryptedContentSample(notebookPath ?: "")
                    if (encryptedContent != null) {
                        val cipher =
                            encryptionManager.getCipherForAccess(encryptedContent, keyAlias)
                        println("🔐 Cipher created successfully with IV")

                        val biometricRequest = BiometricRequest(
                            notebookPath = notebookPath,
                            keyAlias = keyAlias,
                            cipher = cipher,
                            onSuccess = {
                                println("🔐 Biometric success - reloading notes")
                                loadNotes()
                            },
                            onError = {
                                println("🔐 Biometric error")
                                _noteListState.value =
                                    NoteListState.Error("Аутентификация не пройдена")
                            }
                        )
                        //_noteListState.postValue(NoteListState.Blocked(biometricRequest))
                        _biometricRequest.postValue(biometricRequest)
                        _message.postValue("✅ BiometricRequest sent to Fragment")
                        println("✅ BiometricRequest sent to Fragment")
                    } else {
                        println("❌ Could not get encrypted content")
                        _noteListState.value =
                            NoteListState.Error("Ошибка: не найдены зашифрованные данные")
                    }
                } else {
                    println("❌ No key alias found")
                    _noteListState.value =
                        NoteListState.Error("Ошибка безопасности: ключ не найден")
                }
            } catch (e: Exception) {
                println("❌ Error in requestBiometricAuthentication: ${e.message}")
                _noteListState.value = NoteListState.Error("Ошибка безопасности: ${e.message}")
            } finally {
                println("🔐 ViewModel.requestBiometricAuthentication END")
            }
        }
    }

    fun onBiometricSuccess() {
       // _biometricRequest.postValue(null)
        loadNotes()
    }

    fun onBiometricError() {
        //_biometricRequest.postValue(null)
        showMessage("Аутентификация не пройдена")
    }

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

    fun testKeyCreation(notebookPath: String) {
        viewModelScope.launch {
            try {
                val keyAlias = configManager.generateKeyAlias(notebookPath)
                println("🧪 Creating both keys...")

                val keysCreated = encryptionManager.createNotebookKeys(keyAlias)
                println("🧪 Keys creation result: $keysCreated")

                if (keysCreated) {
                    val testText = "Test encryption"
                    val encrypted = encryptionManager.encryptContent(testText, keyAlias)
                    println("🧪 Encryption test - original: '$testText', result: '$encrypted'")
                    println("🧪 Test successful: ${testText != encrypted}")
                }

            } catch (e: Exception) {
                println("🧪 TEST FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun testKeyCreationLast(notebookPath: String) {
        viewModelScope.launch {
            try {
                val keyAlias = configManager.generateKeyAlias(notebookPath)
                println("🧪 TEST KEY CREATION")
                println("🧪 Key alias: $keyAlias")

                // Проверяем до создания
                println("🧪 Checking if key exists before creation...")
                val existsBefore = encryptionManager.keyExists(keyAlias)
                println("🧪 Key exists before: $existsBefore")

                // Создаем ключ
                println("🧪 Creating key...")
                val created = encryptionManager.createKeyForNotebook(keyAlias)
                println("🧪 Key creation result: $created")

                // Проверяем после создания
                println("🧪 Checking if key exists after creation...")
                val existsAfter = encryptionManager.keyExists(keyAlias)
                println("🧪 Key exists after: $existsAfter")

                // Пробуем использовать ключ
                if (existsAfter) {
                    println("🧪 Testing key usage...")
                    val testText = "Test encryption"
                    val encrypted = encryptionManager.encryptContent(testText, keyAlias)
                    println("🧪 Encryption test - original: '$testText', result: '$encrypted'")
                    println("🧪 Test successful: ${testText != encrypted}")
                }

            } catch (e: Exception) {
                println("🧪 TEST FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    fun protectNotebook(notebookPath: String) {
        viewModelScope.launch {
            //_protectionState.value = ProtectionState.Protecting(notebookPath)

            try {
                val keyAlias = configManager.generateKeyAlias(notebookPath)

                // Создаем ОБА ключа
                val keysCreated = encryptionManager.createNotebookKeys(keyAlias)
                if (!keysCreated) {
                    showMessage("Не удалось создать ключи безопасности")
                    //_protectionState.value = ProtectionState.Error("Не удалось создать ключи безопасности")
                    return@launch
                }

                configManager.setNotebookProtected(notebookPath, keyAlias)
                reEncryptExistingNotes(notebookPath)

                //_protectionState.value = ProtectionState.Success("Записная книжка защищена")
                showMessage("Записная книжка защищена")
                loadNotes()

            } catch (e: Exception) {
                showMessage("Ошибка защиты: ${e.message}")
                //_protectionState.value = ProtectionState.Error("Ошибка защиты: ${e.message}")
            }
        }
    }

    fun protectNotebookLast2(notebookPath: String) {
        viewModelScope.launch {
            try {
                val keyAlias = configManager.generateKeyAlias(notebookPath)
                println("🛡️ DEBUG protectNotebook START")
                println("🛡️ Notebook: $notebookPath")
                println("🛡️ Generated key alias: $keyAlias")

                // Проверяем ДО создания
                val keyExistsBefore = encryptionManager.keyExists(keyAlias)
                println("🛡️ Key exists before creation: $keyExistsBefore")

                // Создаем ключ
                println("🛡️ Creating key...")
                val keyCreated = encryptionManager.createKeyForNotebook(keyAlias)
                println("🛡️ Key creation result: $keyCreated")

                // Проверяем ПОСЛЕ создания
                val keyExistsAfter = encryptionManager.keyExists(keyAlias)
                println("🛡️ Key exists after creation: $keyExistsAfter")

                if (!keyExistsAfter) {
                    println("❌ KEY CREATION FAILED!")
                    return@launch
                }

                // Сохраняем конфигурацию
                configManager.setNotebookProtected(notebookPath, keyAlias)
                println("✅ Notebook marked as protected")

                // Перешифровываем
                reEncryptExistingNotes(notebookPath)

            } catch (e: Exception) {
                println("❌ Error in protectNotebook: ${e.message}")
                e.printStackTrace()
            } finally {
                println("🛡️ DEBUG protectNotebook END\n")
            }
        }
    }

    fun protectNotebookLast(notebookPath: String) {
        viewModelScope.launch {
            try {
                // Генерируем уникальный ключ для книжки
                val keyAlias = configManager.generateKeyAlias(notebookPath)
                println("DEBUG: Generated key alias: $keyAlias")

                // Проверяем существует ли ключ ДО создания
//                val keyExistsBefore = encryptionManager.keyExists(keyAlias)
//                println("DEBUG: Key exists before creation: $keyExistsBefore")

                // Создаем ключ
                val keyCreated = encryptionManager.createKeyForNotebook(keyAlias)
                //val keyTestCreated = encryptionManager.createTestKey(keyAlias)
                println("DEBUG: Key creation result: $keyCreated")

                if (!keyCreated) {
                    showMessage("Ошибка создания ключа")
                    return@launch
                }
                // Проверяем существует ли ключ ПОСЛЕ создания
                val keyExistsAfter = encryptionManager.keyExists(keyAlias)
                println("DEBUG: Key exists after creation: $keyExistsAfter")

                if (!keyExistsAfter) {
                    println("DEBUG: KEY WAS NOT CREATED SUCCESSFULLY!")
                    return@launch
                }
                // Сохраняем конфигурацию
                configManager.setNotebookProtected(notebookPath, keyAlias)
                println("DEBUG: Notebook marked as protected")

                // Перешифровываем  существующие заметки в этой книжке
                reEncryptExistingNotes(notebookPath)

                isProtected = configManager.isNotebookProtected(notebookPath)

                val currentState = _noteListState.value
                if (currentState is NoteListState.Success) _noteListState.postValue(
                    currentState.copy(isProtected = isProtected)
                )
                showMessage("Записная книжка защищена")

            } catch (e: Exception) {
                println("DEBUG: Error in protectNotebook: ${e.message}")
                e.printStackTrace()
                showMessage("Ошибка защиты: ${e.message}")
            }
        }
    }


    fun unprotectNotebook(notebookPath: String) {
        viewModelScope.launch {
            try {
                val keyAlias = configManager.getKeyAliasForNotebook(notebookPath)

                if (keyAlias == null) {
                    showMessage("Ключ шифрования не найден")
                    return@launch
                }

                val decryptionSuccess = decryptExistingNotes(notebookPath)

                if (!decryptionSuccess) {
                    showMessage("Не удалось расшифровать заметки")
                    return@launch
                }

                val keyDeleted = encryptionManager.deleteKey(keyAlias)

                if (!keyDeleted) showMessage("Warning: Failed to delete key from Keystore, but continuing...")


                configManager.setNotebookUnprotected(notebookPath)
                isProtected = configManager.isNotebookProtected(notebookPath)

                val currentState = _noteListState.value
                if (currentState is NoteListState.Success) _noteListState.postValue(
                    currentState.copy(isProtected = isProtected)
                )
                showMessage("Защита снята")

            } catch (e: SecurityException) {
                showMessage("Требуется биометрическая аутентификация для снятия защиты")
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

    fun onNoteClicked(noteId: String) =
        _navigationEvent.postValue(NavigationEvent.NavigateToNote(noteId))

    fun onNavigated() = _navigationEvent.postValue(NavigationEvent.Idle)


//    private fun handleBiometricRequired() {
//        // Проверяем что книжка действительно защищена
//        if (!isProtected) {
//            showMessage("Ошибка: книжка не защищена")
//            return
//        }
//
//        val keyAlias = keyAlias
//        if (keyAlias == null) {
//            showMessage("Ошибка: ключ шифрования не найден")
//            return
//        }
//
//        try {
//            val cipher = encryptionManager.getCipherForDecryption(keyAlias)
//
//            _biometricRequest.value = BiometricRequest(
//                notebookPath = notebookPath,
//                keyAlias = keyAlias,
//                cipher = cipher,
//                {
//                    // После успешной биометрии снова загружаем заметки
//                    loadNotes()
//                },
//            ) {
//                _noteListState.postValue(NoteListState.Error("Аутентификация не пройдена"))
//            }
////                onSuccess = { loadNotes() },
////                onError = { showMessage("Аутентификация не пройдена") }
//
//        } catch (e: Exception) {
//            showMessage("Ошибка безопасности: ${e.message}")
//        }
//    }


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