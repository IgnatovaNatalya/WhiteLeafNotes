package ru.whiteleaf.notes.presentation.start

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.CreateNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.recent.GetRecentNotesUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase
import ru.whiteleaf.notes.presentation.note_list.NoteListState
import kotlin.collections.forEach

const val MAX_ITEMS = 3
const val STEP_ITEMS = 3

class StartViewModel(
    private val getNotebooksUseCase: GetNotebooksUseCase,
    private val getNotesUseCase: GetNotesUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val createNotebookUseCase: CreateNotebookUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val updateNoteDateUseCase: UpdateNoteDateUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val deleteNotebookUseCase: DeleteNotebookByPathUseCase,
    private val exportNotebookUseCase: ExportNotebookUseCase,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val isNotebookProtectedUseCase: IsNotebookProtectedUseCase,
    private val getRecentNotesUseCase: GetRecentNotesUseCase,
    private val createKeyForNotebookUseCase: CreateKeyForNotebookUseCase,
    private val deleteKeyForNotebookUseCase: DeleteKeyForNotebookUseCase,
    private val encryptNotebookUseCase: EncryptNotebookUseCase,
    private val decryptNotebookUseCase: DecryptNotebookUseCase
) : ViewModel() {

    private val _navigationEvent = MutableLiveData<StartNavigationEvent>()
    val navigationEvent: LiveData<StartNavigationEvent> = _navigationEvent

    private val _startScreenState = MutableLiveData<StartScreenState>()
    val startScreenState: LiveData<StartScreenState> = _startScreenState

    private var recentList: List<RecentNote> = emptyList()
    private var notebookList: List<Notebook> = emptyList()
    private var rootNoteList: List<Note> = emptyList()

    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private var visibleRecentCount = MAX_ITEMS
    private var visibleNotebooksCount = MAX_ITEMS
    private var visibleNotesCount = MAX_ITEMS

    val exportPath =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).name} / $DEFAULT_DIR"

    init {
        loadData()
    }

    fun getAllNotebooks(): List<Notebook> = notebookList

    fun loadData() {

        println("DEBUG: StartVM: loading data")

        _startScreenState.postValue(StartScreenState.Loading)

        viewModelScope.launch {
            try {
                recentList = getRecentNotesUseCase()
                notebookList = getNotebooksUseCase()

                val rootNotes = getNotesUseCase(null) // Заметки в корневой папке

                rootNotes.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        _message.postValue("Пустая заметка удалена")
                    }
                }

                rootNoteList = rootNotes.filter { it.isNotEmpty() }

                buildStartItems()

            } catch (e: Exception) {
                _message.value = "Ошибка загрузки данных: ${e.message}"
            }
        }
    }

    fun buildStartItems() {
        val items = mutableListOf<StartListItem>()

        // Секция Недавние
        val recentToShow = recentList.take(visibleRecentCount)
        println("DEBUG: StartVM: build start items, recent items count: ${recentList.size}, showing ${recentToShow.size}")

        if (recentToShow.isNotEmpty()) {
            items.add(StartListItem.HeaderRecent)
            items.add(StartListItem.Divider)

            recentToShow.forEachIndexed { index, note ->
                items.add(StartListItem.RecentNoteItem(note))

                if (index < recentToShow.lastIndex)
                    items.add(StartListItem.Divider)
                else
                    if (recentToShow.size < recentList.size) items.add(StartListItem.ShowMoreRecent) else items.add(
                        StartListItem.Divider
                    )
            }
        }

        // Секция записных книжек
        val notebooksToShow = notebookList.take(visibleNotebooksCount)
        println("DEBUG: StartVM: build start items, notebooks items count: ${notebookList.size} , showing ${notebooksToShow.size}")

        items.add(StartListItem.HeaderNotebooks)
        items.add(StartListItem.Divider)

        if (notebooksToShow.isEmpty()) {
            items.add(StartListItem.EmptyNotebooks)
        } else {
            notebooksToShow.forEachIndexed { index, notebook ->
                items.add(StartListItem.NotebookItem(notebook))

                if (index < notebooksToShow.lastIndex)
                    items.add(StartListItem.Divider)
                else
                    if (notebooksToShow.size < notebookList.size) items.add(StartListItem.ShowMoreNotebooks)
                    else items.add(StartListItem.Divider)
            }
        }

        // Секция заметок
        val rootNotesToShow = rootNoteList.take(visibleNotesCount)
        println("DEBUG: StartVM: build start items, rootNotes items count: ${rootNoteList.size}, showing ${rootNotesToShow.size}")

        items.add(StartListItem.HeaderRootNotes)
        items.add(StartListItem.Divider)

        if (rootNotesToShow.isEmpty()) {
            items.add(StartListItem.EmptyNotes)
        } else {
            rootNotesToShow.forEachIndexed { index, note ->
                items.add(StartListItem.NoteItem(note))
                if (index < rootNotesToShow.lastIndex)
                    items.add(StartListItem.Divider)
                else
                    if (rootNotesToShow.size < rootNoteList.size) items.add(StartListItem.ShowMoreNotes)
                    else items.add(StartListItem.Divider)
            }
        }

        _startScreenState.postValue(StartScreenState.Success(items))
    }

    fun showMoreRecent() {
        visibleRecentCount += STEP_ITEMS
        buildStartItems()
    }

    fun showMoreNotebooks() {
        visibleNotebooksCount += STEP_ITEMS
        buildStartItems()
    }

    fun showMoreNotes() {
        visibleNotesCount += STEP_ITEMS
        buildStartItems()
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(null)
                _navigationEvent.postValue(StartNavigationEvent.NavigateToCreatedNote(newNote))
                _message.postValue("Заметка создана")
            } catch (e: Exception) {
                _message.postValue("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNoteUseCase(note, newTitle)
                    loadData()
                    _message.postValue("Название заметки изменено")
                }
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun updateNoteDate(note: Note, newDate: Long) {
        viewModelScope.launch {
            try {
                updateNoteDateUseCase(note, newDate)
                loadData()
                _message.postValue("Дата заметки обновлена")
            } catch (e: Exception) {
                _message.postValue("Ошибка обновления даты: ${e.message}")
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
                    _message.postValue("Не удалось подтвердить личность")
                    return@launch
                }

                if (newName != notebook.path) {
                    renameNotebookUseCase(notebook.path, newName)
                    loadData()
                    _message.postValue("Название записной книжки изменено")
                }
            } catch (e: AuthenticationRequiredException) {
                _message.postValue("Записная книжка заблокирована: ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun moveNote(context: Context, note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                val unlocked =
                    if (targetNotebookPath != null)
                        if (isNotebookProtectedUseCase(targetNotebookPath)) unlockNotebookUseCase(
                            targetNotebookPath, context, title = "Целевая записная книжка защищена",
                            reason = "Для перемещения"
                        ) else true
                    else true
                if(unlocked) {
                    moveNoteUseCase(note, targetNotebookPath)
                    loadData()
                }
            } catch (e: AuthenticationRequiredException) {
                _message.postValue("Ошибка разблокировки")
                println("DEBUG: StartVM: moveNote: AuthenticationRequiredException ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                loadData()
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    fun encryptNotebook(context: Context, notebook: Notebook) {
        viewModelScope.launch {
            try {
                if (isNotebookProtectedUseCase(notebook.path)) {
                    _message.postValue("Записная книжка уже защищищена")
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
                    _message.postValue ( "Записная книжка защищена")
                }
                else _message.postValue("Отмена установки защиты")

            } catch (e: AuthenticationRequiredException) {
                _message.postValue("Не удалось зашифровать записную книжку: ${e.message}")
                println("DEBUG: StartVM fail encrypt notebook: ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка шифрования: ${e.message}")
                println("DEBUG: StartVM: Ошибка шифрования: ${e.message}")
            }
        }
    }

    fun decryptNotebook(context: Context, notebook: Notebook) {
        viewModelScope.launch {
            try {
                if (!isNotebookProtectedUseCase(notebook.path)) {
                    _message.postValue("Защита не установлена")
                    return@launch
                }
                val unlocked =
                    unlockNotebookUseCase(notebook.path, context, reason = "Для снятия защиты")
                if (!unlocked) {
                    _message.postValue("Не удалось подтвердить личность")
                    return@launch
                }
                decryptNotebookUseCase(notebook.path)
                deleteKeyForNotebookUseCase(notebook.path)
                loadData()
                _message.value = "Защита записной книжки снята"
            } catch (e: Exception) {
                _message.postValue("Ошибка расшифровки: ${e.message}")
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
                _message.postValue("Архив успешно создан")
                if (shareFile)
                    _navigationEvent.postValue(StartNavigationEvent.ShareUri(result.getOrNull()))
            } else {
                _message.postValue("Отмена экспорта")
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
                    _message.postValue("Записная книжка удалена")
                } else {
                    _message.postValue("Не удалось подтвердить личность")
                    return@launch
                }

            } catch (e: AuthenticationRequiredException) {
                _message.postValue("Записная книжка заблокирована: ${e.message}")
            } catch (e: Exception) {
                _message.postValue("Ошибка удаления записной книжки: ${e.message}")
            }
        }
    }

    fun onNavigated() = _navigationEvent.postValue(StartNavigationEvent.Idle)

    fun clearMessage() = _message.postValue(null)

}