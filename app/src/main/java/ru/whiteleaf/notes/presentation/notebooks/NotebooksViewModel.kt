package ru.whiteleaf.notes.presentation.notebooks

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.use_case.notebooks.CreateNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.PinNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.UnpinNotebookUseCase

class NotebooksViewModel(
    private val getNotebooksUseCase: GetNotebooksUseCase,
    private val createNotebookUseCase: CreateNotebookUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val deleteNotebookUseCase: DeleteNotebookByPathUseCase,
    private val exportNotebookUseCase: ExportNotebookUseCase,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val isNotebookProtectedUseCase: IsNotebookProtectedUseCase,
    private val createKeyForNotebookUseCase: CreateKeyForNotebookUseCase,
    private val deleteKeyForNotebookUseCase: DeleteKeyForNotebookUseCase,
    private val encryptNotebookUseCase: EncryptNotebookUseCase,
    private val decryptNotebookUseCase: DecryptNotebookUseCase,
    private val pinNotebookUseCase: PinNotebookUseCase,
    private val unpinNotebookUseCase: UnpinNotebookUseCase

) : ViewModel() {

    private val _navigationEvent = MutableLiveData<NotebooksNavigationEvent>()
    val navigationEvent: LiveData<NotebooksNavigationEvent> = _navigationEvent

    private val _notebooksScreenState = MutableLiveData<NotebooksScreenState>()
    val notebooksScreenState: LiveData<NotebooksScreenState> = _notebooksScreenState


    val exportPath =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).name} / $DEFAULT_DIR"

    init {
        loadData()
    }

    fun loadData() {

        println("DEBUG: NotebooksVM: loading data")
        _notebooksScreenState.postValue(NotebooksScreenState.Loading)

        viewModelScope.launch {
            try {
                val notebookList = getNotebooksUseCase()
                _notebooksScreenState.postValue(NotebooksScreenState.Success(notebookList))
            } catch (e: Exception) {
                _notebooksScreenState.postValue(NotebooksScreenState.Error("Ошибка загрузки данных: ${e.message}"))
            }
        }
    }

    fun postMessage(msg: String) {
        _navigationEvent.postValue(NotebooksNavigationEvent.ShowMessage(msg))
    }

    fun createNotebook(path: String) {
        viewModelScope.launch {
            try {
                val newNotebook = createNotebookUseCase(path)
                _navigationEvent.postValue(
                    NotebooksNavigationEvent.NavigateToCreatedNotebook(
                        newNotebook
                    )
                )

            } catch (e: Exception) {
                postMessage("Ошибка создания записной книжки: ${e.message}")
            }
        }
    }

    fun renameNotebook(notebook: Notebook, newName: String, context: Context) {
        viewModelScope.launch {
            try {
                val unlocked = if (isNotebookProtectedUseCase(notebook.path)) {
                    unlockNotebookUseCase(
                        notebook.path,
                        context,
                        reason = "Для переименования"
                    )
                } else true

                if (!unlocked) {
                    postMessage("Не удалось подтвердить личность")
                    return@launch
                }

                if (newName != notebook.path) {
                    renameNotebookUseCase(notebook.path, newName)
                    loadData()
                    postMessage("Название записной книжки изменено")
                }
            } catch (e: AuthenticationRequiredException) {
                postMessage("Записная книжка заблокирована: ${e.message}")
            } catch (e: Exception) {
                postMessage("Ошибка переименования: ${e.message}")
            }
        }
    }


    fun encryptNotebook(context: Context, notebook: Notebook) {
        viewModelScope.launch {
            try {
                if (isNotebookProtectedUseCase(notebook.path)) {
                    postMessage("Записная книжка уже защищищена")
                    return@launch
                }
                val locked = unlockNotebookUseCase(
                    notebook.path,
                    context,
                    title = "Защита записной книжки",
                    reason = "Для защиты"
                )
                if (locked) {
                    createKeyForNotebookUseCase(notebook.path)
                    encryptNotebookUseCase(notebook.path)
                    loadData()
                    postMessage("Записная книжка защищена")
                } else postMessage("Отмена установки защиты")

            } catch (e: AuthenticationRequiredException) {
                postMessage("Не удалось зашифровать записную книжку: ${e.message}")
                println("DEBUG: NotebooksVM fail encrypt notebook: ${e.message}")
            } catch (e: Exception) {
                postMessage("Ошибка шифрования: ${e.message}")
                println("DEBUG: NotebooksVM: Ошибка шифрования: ${e.message}")
            }
        }
    }

    fun decryptNotebook(context: Context, notebook: Notebook) {
        viewModelScope.launch {
            try {
                if (!isNotebookProtectedUseCase(notebook.path)) {
                    postMessage("Защита не установлена")
                    return@launch
                }
                val unlocked =
                    unlockNotebookUseCase(notebook.path, context, reason = "Для снятия защиты")
                if (!unlocked) {
                    postMessage("Не удалось подтвердить личность")
                    return@launch
                }
                decryptNotebookUseCase(notebook.path)
                deleteKeyForNotebookUseCase(notebook.path)
                loadData()
                postMessage("Защита записной книжки снята")
            } catch (e: Exception) {
                postMessage("Ошибка расшифровки: ${e.message}")
            }
        }
    }

    fun exportNotebook(
        context: Context,
        notebookPath: String,
        shareFile: Boolean,
        password: String?
    ) {
        viewModelScope.launch {
            val result = exportNotebookUseCase(context, notebookPath, password)

            if (result.isSuccess) {
                postMessage("Архив успешно создан")
                if (shareFile)
                    _navigationEvent.postValue(NotebooksNavigationEvent.ShareUri(result.getOrNull()))
            } else {
                postMessage("Отмена экспорта")
            }
        }
    }

    fun deleteNotebook(notebook: Notebook, context: Context) {
        viewModelScope.launch {
            try {
                val unlocked = if (isNotebookProtectedUseCase(notebook.path)) {
                    unlockNotebookUseCase(notebook.path, context, reason = "Для удаления")
                } else
                    true

                if (unlocked) {
                    deleteNotebookUseCase(notebook.path)
                    loadData()
                    postMessage("Записная книжка удалена")
                } else {
                    postMessage("Не удалось подтвердить личность")
                    return@launch
                }

            } catch (e: AuthenticationRequiredException) {
                postMessage("Записная книжка заблокирована: ${e.message}")
            } catch (e: Exception) {
                postMessage("Ошибка удаления записной книжки: ${e.message}")
            }
        }
    }

    fun pinNotebook(notebook: Notebook) {
        pinNotebookUseCase(notebook.path)
        loadData()
    }

    fun unpinNotebook(notebook: Notebook) {
        unpinNotebookUseCase(notebook.path)
        loadData()
    }

    fun clearEvent() = _navigationEvent.postValue(NotebooksNavigationEvent.Idle)

}