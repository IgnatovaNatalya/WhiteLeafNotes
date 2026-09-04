package ru.whiteleaf.notes.presentation.start

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
import ru.whiteleaf.notes.domain.model.printDebug
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.recent.GetRecentNotesUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.PinNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.UnpinNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.FindNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase
import ru.whiteleaf.notes.presentation.note_list.NoteListState
import ru.whiteleaf.notes.presentation.search.SearchListItem
import java.io.IOException
import java.security.InvalidKeyException
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
    private val decryptNotebookUseCase: DecryptNotebookUseCase,
    private val pinNotebookUseCase: PinNotebookUseCase,
    private val unpinNotebookUseCase: UnpinNotebookUseCase,
    private val findNotesUseCase: FindNotesUseCase,

    ) : ViewModel() {

    private val _navigationEvent = MutableLiveData<StartNavigationEvent>()
    val navigationEvent: LiveData<StartNavigationEvent> = _navigationEvent

    private val _startScreenState = MutableLiveData<StartScreenState>()
    val startScreenState: LiveData<StartScreenState> = _startScreenState

    private var recentList: List<RecentNote> = emptyList()
    private var notebookList: List<Notebook> = emptyList()
    private var rootNoteList: List<Note> = emptyList()

    private var visibleRecentCount = MAX_ITEMS
    private var visibleNotebooksCount = MAX_ITEMS
    private var visibleNotesCount = MAX_ITEMS

    val exportPath =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).name} / $DEFAULT_DIR"

    private var searchDebounceJob: Job? = null
    var searchQuery: String? = null

    init {
        loadData()
    }

    fun resumeScreen() {
        when (_startScreenState.value) {
            is StartScreenState.Success -> loadData()
            is StartScreenState.SearchResults -> if (searchQuery != null) findNotes()
            else -> {}
        }
    }

    fun getAllNotebooks(): List<Notebook> = notebookList

    fun prepareSearch() = _startScreenState.postValue(StartScreenState.Idle)

    fun loadData() {

        println("DEBUG: StartVM: loading data")
        _startScreenState.postValue(StartScreenState.Loading)

        viewModelScope.launch {
            try {
                val rootNotes = getNotesUseCase(null)

                rootNotes.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        postMessage("Пустая заметка удалена")
                    }
                }

                rootNoteList = rootNotes.filter { it.isNotEmpty() }

                recentList = getRecentNotesUseCase()
                notebookList = getNotebooksUseCase()

                buildStartItems()

            } catch (e: Exception) {
                postMessage("Ошибка загрузки данных: ${e.message}")
            }
        }
    }

    private fun findNotes() {
        println("DEBUG: StartVM: Поиск заметок, searchQuery=$searchQuery") //todo сделать еще постранично
        val query = searchQuery ?: return

        viewModelScope.launch {
            _startScreenState.postValue(StartScreenState.Loading)

            try {
                val items = mutableListOf<SearchListItem>()

                val foundNotebooks =
                    notebookList.filter { it.path.lowercase().contains(query) }.also { list ->
                        list.forEach { items.add(SearchListItem.SearchNotebook(it, query)) }
                    }

                println("DEBUG: StartVM: findNotebooks: ${foundNotebooks.size} notebooks found")

                val foundNotes = findNotesUseCase(
                    null, query,
                    notebookList
                )

                println("DEBUG: StartVM: findNotes: ${foundNotes.size} notes found")
                foundNotes.forEach { note -> note.printDebug() }

                foundNotes.forEach { foundNote ->
                    if (foundNote.foundedInTitle) items.add(
                        SearchListItem.SearchListNoteTitle(foundNote)
                    )
                    else items.add(SearchListItem.SearchListNoteContent(foundNote))
                }
                _startScreenState.postValue(StartScreenState.SearchResults(query, items))
            } catch (_: AuthenticationRequiredException) {
                //поймали зашифрованную но ничего не делаем
            } catch (e: IOException) {
                postMessage("Ошибка поиска заметок: ${e.message}")
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    //тоже ничего не делаем
                    println("DEBUG: Start VM: InvalidKeyException ${e.message}")
                } else
                    postMessage(e.message ?: "Ошибка поиска")
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        searchQuery = query
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(500)
            findNotes()
        }
    }

    fun onSearchQuerySubmitted(query: String) {
        searchDebounceJob?.cancel()
        searchQuery = query
        findNotes()
    }

    fun buildStartItems() {
        val items = mutableListOf<StartListItem>()

        // Секция Недавние
        val recentToShow = recentList.take(visibleRecentCount)
        println("DEBUG: StartVM: build start items, recent items count: ${recentList.size}, showing ${recentToShow.size}")

        if (recentToShow.isNotEmpty()) {
            items.add(StartListItem.HeaderRecent)
            items.add(StartListItem.DividerHeader)

            recentToShow.forEachIndexed { index, note ->
                items.add(StartListItem.RecentNoteItem(note))

                if (index < recentToShow.lastIndex)
                    items.add(StartListItem.DividerLineBg)
                else
                    if (recentToShow.size < recentList.size) items.add(StartListItem.ShowMoreRecent) else items.add(
                        StartListItem.DividerAfter
                    )
            }
        }

        // Секция записных книжек
        val notebooksToShow = notebookList.take(visibleNotebooksCount)
        println("DEBUG: StartVM: build start items, notebooks items count: ${notebookList.size} , showing ${notebooksToShow.size}")

        items.add(StartListItem.HeaderNotebooks)

        if (notebooksToShow.isEmpty()) {
            items.add(StartListItem.DividerLine)
            items.add(StartListItem.CreateNotebook)
            items.add(StartListItem.DividerLine)
        } else {
            items.add(StartListItem.DividerHeader)
            notebooksToShow.forEachIndexed { index, notebook ->
                items.add(StartListItem.NotebookItem(notebook))

                if (index < notebooksToShow.lastIndex)
                    items.add(StartListItem.DividerLineBg)
                else
                    if (notebooksToShow.size < notebookList.size) {
                        items.add(StartListItem.ShowMoreNotebooks)
                        items.add(StartListItem.CreateNotebook)
                        items.add(StartListItem.DividerLine)
                    } else {
                        items.add(StartListItem.DividerLineBg)
                        items.add(StartListItem.CreateNotebook)
                        items.add(StartListItem.DividerLine)
                    }
            }
        }

        // Секция заметок
        val rootNotesToShow = rootNoteList.take(visibleNotesCount)

        if (rootNotesToShow.isNotEmpty()) {
            println("DEBUG: StartVM: build start items, rootNotes items count: ${rootNoteList.size}, showing ${rootNotesToShow.size}")

            items.add(StartListItem.HeaderRootNotes)
            items.add(StartListItem.DividerHeader)

            rootNotesToShow.forEachIndexed { index, note ->
                items.add(StartListItem.NoteItem(note))
                if (index < rootNotesToShow.lastIndex)
                    items.add(StartListItem.DividerLineBg)
                else
                    if (rootNotesToShow.size < rootNoteList.size) items.add(StartListItem.ShowMoreNotes)
                    else items.add(StartListItem.DividerAfter)

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
            } catch (e: Exception) {
                postMessage("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun createNotebook(path: String) {
        viewModelScope.launch {
            try {
                val newNotebook = createNotebookUseCase(path)
                _navigationEvent.postValue(
                    StartNavigationEvent.NavigateToCreatedNotebook(
                        newNotebook
                    )
                )

            } catch (e: Exception) {
                postMessage("Ошибка создания записной книжки: ${e.message}")
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNoteUseCase(note, newTitle)
                    loadData()
                    postMessage("Название заметки изменено")
                }
            } catch (e: Exception) {
                postMessage("Ошибка переименования: ${e.message}")
            }
        }
    }

    fun updateNoteDate(note: Note, newDate: Long) {
        viewModelScope.launch {
            try {
                updateNoteDateUseCase(note, newDate)
                loadData()
                postMessage("Дата заметки обновлена")
            } catch (e: Exception) {
                postMessage("Ошибка обновления даты: ${e.message}")
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
                if (unlocked) {
                    moveNoteUseCase(note, targetNotebookPath)
                    loadData()
                }
            } catch (e: AuthenticationRequiredException) {
                postMessage("Ошибка разблокировки")
                println("DEBUG: StartVM: moveNote: AuthenticationRequiredException ${e.message}")
            } catch (e: Exception) {
                postMessage("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                loadData()
            } catch (e: Exception) {
                postMessage("Ошибка удаления заметки: ${e.message}")
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
                println("DEBUG: StartVM fail encrypt notebook: ${e.message}")
            } catch (e: Exception) {
                postMessage("Ошибка шифрования: ${e.message}")
                println("DEBUG: StartVM: Ошибка шифрования: ${e.message}")
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
                    _navigationEvent.postValue(StartNavigationEvent.ShareUri(result.getOrNull()))
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

    private fun postMessage(msg: String) =
        _navigationEvent.postValue(StartNavigationEvent.ShowMessage(msg))

    fun clearEvent() {
        _navigationEvent.postValue(StartNavigationEvent.Idle)
    }
}