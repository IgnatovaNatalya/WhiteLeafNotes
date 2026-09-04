package ru.whiteleaf.notes.presentation.note_list

import android.content.Context
import android.os.Environment
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.notes.CreateNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.DeleteNotebookByPathUseCase
import ru.whiteleaf.notes.domain.use_case.share.ExportNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.RenameNotebookUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.common.AppConstants.DEFAULT_DIR
import ru.whiteleaf.notes.domain.interactor.SettingsInteractor
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.model.printDebug
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.CreateKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DecryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.DeleteKeyForNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.EncryptNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookUnlockedUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.LockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notes.FindNotesUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase
import ru.whiteleaf.notes.presentation.search.SearchListItem
import java.io.IOException
import java.security.InvalidKeyException

class NoteListViewModel(
    private val getNotesUseCase: GetNotesUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val createNoteUseCase: CreateNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val updateNoteDateUseCase: UpdateNoteDateUseCase,
    private val renameNotebookUseCase: RenameNotebookUseCase,
    private val exportNotebookUseCase: ExportNotebookUseCase,
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
    private val getNotebooksUseCase: GetNotebooksUseCase,
    private val findNotesUseCase: FindNotesUseCase,
    private val notebookPath: String?
) : ViewModel() {

    private val _noteListState = MutableLiveData<NoteListState>()
    val noteListState: LiveData<NoteListState> = _noteListState

    private val _navigationEvent = MutableLiveData<NoteListNavigationEvent>()
    val navigationEvent: LiveData<NoteListNavigationEvent> = _navigationEvent

    private val _isPlannerView = MutableLiveData(false)

    private var notebooksList: List<Notebook> = emptyList()
    private var isEncrypted = false

    val exportPath =
        "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).name} / $DEFAULT_DIR"

    private var searchDebounceJob: Job? = null
    var searchQuery: String? = null


    init {
        isEncrypted = isNotebookProtectedUseCase(notebookPath ?: "")

        loadViewMode()
        loadNotes()
        saveLastOpenedNotebook()
        loadNotebooks()
    }

    fun resumeScreen() {
        when (_noteListState.value) {
            is NoteListState.Success -> loadNotes()
            is NoteListState.SearchResults -> {if (searchQuery != null) findNotes()  }//if (searchQuery == null) loadNotes()
                //if (searchQuery != null) findNotes() else loadNotes()
            else -> {}
        }
    }

    fun getEncryptionStatus(): Boolean = isEncrypted

    private fun loadNotebooks() {
        viewModelScope.launch {
            try {
                notebooksList = getNotebooksUseCase()
            } catch (e: Exception) {
                println("DEBUG: NoteListVm: loadNotebooks: Error loading notebooks: ${e.message}")
            }
        }
    }

    fun getAllNotebooks(): List<Notebook> = notebooksList

    private fun loadViewMode() {
        if (notebookPath == null) return
        val savedMode = preferencesInteractor.getViewMode(notebookPath)
        _isPlannerView.value = savedMode
    }

    fun setViewMode(isPlanner: Boolean) {
        if (notebookPath == null) return
        _isPlannerView.value = isPlanner
        preferencesInteractor.saveViewMode(notebookPath, isPlanner)
        _navigationEvent.postValue(NoteListNavigationEvent.ReopenNotebook(notebookPath))///
    }

    fun getViewMode(): Boolean {
        return _isPlannerView.value ?: false
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

    private fun findNotes() {
        val query = searchQuery ?: return

        println("DEBUG: NoteListVM: Поиск заметок, query=$query") //todo сделать еще постранично

        viewModelScope.launch {
            _noteListState.postValue(NoteListState.Loading)

            try {
                val foundNotes = findNotesUseCase(
                    notebookPath,
                    query,
                    notebooksList.filter { it.path == notebookPath })

                println("DEBUG: NoteListM: findNotes: ${foundNotes.size} notes found")
                foundNotes.forEach { note -> note.printDebug() }

                val items = mutableListOf<SearchListItem>()

                foundNotes.forEach { foundNote ->
                    if (foundNote.foundedInTitle) items.add(
                        SearchListItem.SearchListNoteTitle(foundNote)
                    )
                    else items.add(SearchListItem.SearchListNoteContent(foundNote))
                }
                _noteListState.postValue(NoteListState.SearchResults(query, items))
            } catch (_: AuthenticationRequiredException) {
                _navigationEvent.postValue(NoteListNavigationEvent.ShowBiometric(UnlockTarget.ToSearch))
            } catch (e: IOException) {
                _noteListState.postValue(NoteListState.Error("Ошибка поиска заметок: ${e.message}"))
            } catch (e: Exception) {
                if (e.cause is InvalidKeyException) {
                    _noteListState.postValue(NoteListState.Blocked)
                    if (notebookPath != null) lockNotebookUseCase(notebookPath)
                    println("DEBUG: NoteListViewmodel: InvalidKeyException ${e.message}")
                } else
                    _noteListState.postValue(NoteListState.Error(e.message ?: "Ошибка поиска"))
            }
        }
    }

    fun prepareSearch() = _noteListState.postValue(NoteListState.Idle)

    fun loadNotes() {
        viewModelScope.launch {
            _noteListState.postValue(NoteListState.Loading)
            var isProtected = false

            try {
                if (notebookPath != null) {

                    isProtected = isEncrypted
                    val isUnlocked = isNotebookUnlockedUseCase(notebookPath)

                    if (isProtected && !isUnlocked) {
                        _noteListState.postValue(NoteListState.Blocked)
                        _navigationEvent.postValue(
                            NoteListNavigationEvent.ShowBiometric(UnlockTarget.ToLoad)
                        )
                        return@launch
                    }
                }

                println("DEBUG: NoteListVM: Загрузка заметок, isProtected= $isProtected")
                val notesList = getNotesUseCase(notebookPath)

                notesList.forEach { note ->
                    if (note.isEmpty()) {
                        deleteNoteUseCase(note)
                        postMessage("Пустая заметка удалена")
                    }
                }

                //notesList.forEach { note ->  println("DEBUG: NoteListVM: note:${note.printDebugIdTitlePath()}")}

                _noteListState.postValue(
                    NoteListState.Success(notesList.filter { it.isNotEmpty() })
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
                    _noteListState.postValue(
                        NoteListState.Error(
                            e.message ?: "Ошибка загрузки"
                        )
                    )
            } finally {
                println("DEBUG: NoteListViewmodel: Окончание загрузки заметок")
            }
        }
    }

    fun unlockNotebook(context: Context, target: UnlockTarget) {
        val isProtected = isEncrypted

        println("DEBUG: unlock notebook, notebookPath = $notebookPath, isProtected = $isProtected")

        if (notebookPath != null && isProtected)
            viewModelScope.launch {
                val unlocked =
                    unlockNotebookUseCase(notebookPath, context, reason = target.toMessage())
                if (unlocked) {
                    println("DEBUG: unlock notebook: success")
                    when (target) {
                        UnlockTarget.ToCreate -> createNewNote()
                        UnlockTarget.ToLoad -> loadNotes()
                        is UnlockTarget.ToSearch -> findNotes()
                    }
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
                if (isEncrypted) {
                    _noteListState.value = NoteListState.Error("Записная книжка уже защищена")
                    return@launch
                }

                val locked = unlockNotebookUseCase(
                    notebookPath,
                    context,
                    title = "Защита записной книжки",
                    reason = "Для защиты"
                )
                if (locked) {
                    createKeyForNotebookUseCase(notebookPath)
                    encryptNotebookUseCase(notebookPath)
                    loadNotes()
                    postMessage("Записная книжка защищена")
                } else postMessage("Отмена установки защиты")

            } catch (e: AuthenticationRequiredException) {
                _noteListState.value =
                    NoteListState.Error("Не удалось зашифровать записную книжку: ${e.message}")

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

                if (isEncrypted) {
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
                postMessage("Защита записной книжки снята")
            } catch (e: Exception) {
                _noteListState.value = NoteListState.Error("Ошибка расшифровки: ${e.message}")
            }
        }
    }

    fun createNewNote() {
        viewModelScope.launch {
            try {
                val newNote = createNoteUseCase(notebookPath)
                _navigationEvent.postValue(NoteListNavigationEvent.NavigateToNote(newNote.id))
            } catch (_: AuthenticationRequiredException) {
                postMessage("Записная книжка заблокирована")
                _navigationEvent.postValue(
                    NoteListNavigationEvent.ShowBiometric(UnlockTarget.ToCreate)
                )
            } catch (e: Exception) {
                postMessage("Ошибка создания заметки: ${e.message}")
            }
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                loadNotes()
            } catch (e: Exception) {
                postMessage("Ошибка удаления заметки: ${e.message}")
            }
        }
    }

    fun moveNote(context: Context, note: Note, targetNotebookPath: String?) {
        viewModelScope.launch {
            try {
                val unlocked =
                    if (targetNotebookPath != null)
                        if (isNotebookProtectedUseCase(targetNotebookPath)) unlockNotebookUseCase(
                            targetNotebookPath,
                            context,
                            title = "Целевая записная книжка защищена",
                            reason = "Для перемещения"
                        ) else true
                    else true

                if (unlocked) {
                    moveNoteUseCase(note, targetNotebookPath)
                    loadNotes()
                } else {
                    _noteListState.postValue(NoteListState.Blocked)
                    postMessage("Отмена перемещения")
                }
            } catch (e: AuthenticationRequiredException) {
                _noteListState.postValue(NoteListState.Blocked)
                postMessage("Ошибка разблокировки ${e.message}")
                println("DEBUG: NoteListVM: moveNote: AuthenticationRequiredException ${e.message}")
            } catch (e: Exception) {
                postMessage("Ошибка перемещения заметки: ${e.message}")
            }
        }
    }

    fun updateNoteTitle(note: Note, newTitle: String) {
        viewModelScope.launch {
            try {
                if (newTitle != note.title) {
                    renameNoteUseCase(note, newTitle)
                    loadNotes()
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
                loadNotes()
                postMessage("Дата заметки изменена")
            } catch (e: Exception) {
                postMessage("Ошибка обновления даты: ${e.message}")
            }
        }
    }

    fun renameNotebook(newName: String, context: Context) {
        if (notebookPath != null)
            viewModelScope.launch {
                try {
                    val unlocked =
                        if (isEncrypted) {
                            unlockNotebookUseCase(
                                notebookPath,
                                context,
                                reason = "Для переименования"
                            )
                        } else true

                    if (!unlocked) {
                        postMessage("Не удалось подтвердить личность")
                        return@launch
                    }

                    if (newName != notebookPath) {
                        renameNotebookUseCase(notebookPath, newName)
                        postMessage("Название записной книжки изменено")
                        _navigationEvent.postValue(
                            NoteListNavigationEvent.ReopenNotebook(
                                newName
                            )
                        )
                    } else postMessage("Ошибка переименования")
                } catch (e: AuthenticationRequiredException) {
                    postMessage("Записная книжка заблокирована: ${e.message}")
                } catch (e: Exception) {
                    postMessage("Ошибка переименования: ${e.message}")
                }
            }
    }

    fun exportNotebook(context: Context, shareFile: Boolean, password: String?) {
        viewModelScope.launch {
            val result = exportNotebookUseCase(context, notebookPath ?: "", password)

            if (result.isSuccess) {
                if (shareFile)
                    _navigationEvent.postValue(NoteListNavigationEvent.ExportLink(result.getOrNull()))
                else
                    postMessage("Архив успешно сохранен")
            } else {
                postMessage("Отмена разблокировки")
            }
        }
    }

    fun deleteNotebook(context: Context) {
        if (notebookPath != null)
            viewModelScope.launch {
                try {
                    if (isEncrypted) {
                        val unlocked =
                            unlockNotebookUseCase(
                                notebookPath,
                                context,
                                reason = "Для удаления"
                            )

                        if (!unlocked) {
                            postMessage("Не удалось подтвердить личность")
                            return@launch
                        }
                    }

                    deleteNotebookUseCase(notebookPath)

                    _navigationEvent.postValue(NoteListNavigationEvent.NavigateUp)
                    postMessage("Записная книжка удалена")

                } catch (e: Exception) {
                    postMessage("Ошибка удаления записной книжки: ${e.message}")
                }
            }
    }

    fun onNoteClicked(noteId: String) =
        _navigationEvent.postValue(NoteListNavigationEvent.NavigateToNote(noteId))

    fun onNoteFoundClicked(noteId: String, contentPosition: Int) =
        _navigationEvent.postValue(
            NoteListNavigationEvent.NavigateToNoteFound(
                noteId,
                contentPosition, searchQuery
            )
        )


    private fun postMessage(msg: String) =
        _navigationEvent.postValue(NoteListNavigationEvent.ShowMessage(msg))

    fun clearEvent() {
        _navigationEvent.postValue(NoteListNavigationEvent.Idle)
    }

    private fun saveLastOpenedNotebook() {
        notebookPath?.let { preferencesInteractor.saveLastOpenedNotebook(notebookPath) }
    }

    fun onNotebookExited(toNote: Boolean) {
        println("DEBUG: Выход из блокнота $notebookPath, toNote=$toNote")
        val state = _noteListState.value

        if (toNote || state !is NoteListState.Success) return
        println("DEBUG: isEncrypted = $isEncrypted ")

        if (notebookPath != null)
            if (isEncrypted) {
                println("DEBUG: убираем признак разблокировки при выходе: $notebookPath")
                lockNotebookUseCase(notebookPath)
            }
    }

    override fun onCleared() {
        super.onCleared()
        onNotebookExited(false)
    }
}