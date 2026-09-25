package ru.whiteleaf.notes.presentation.note_edit

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.use_case.notes.DeleteNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.GetNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.MoveNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.RenameNoteUseCase
import ru.whiteleaf.notes.domain.use_case.notes.SaveNoteContentUseCase
import ru.whiteleaf.notes.domain.use_case.share.ShareNoteFileUseCase
import kotlinx.coroutines.launch
import ru.whiteleaf.notes.common.utils.highlightNoteMatches
import ru.whiteleaf.notes.domain.interactor.SettingsInteractor
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.domain.repository.AuthenticationRequiredException
import ru.whiteleaf.notes.domain.use_case.encryption.IsNotebookProtectedUseCase
import ru.whiteleaf.notes.domain.use_case.recent.RemoveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.recent.SaveRecentNoteUseCase
import ru.whiteleaf.notes.domain.use_case.encryption.UnlockNotebookUseCase
import ru.whiteleaf.notes.domain.use_case.notebooks.GetNotebooksUseCase
import ru.whiteleaf.notes.domain.use_case.notes.UpdateNoteDateUseCase

class NoteEditViewModel(
    private val getNoteUseCase: GetNoteUseCase,
    private val deleteNoteUseCase: DeleteNoteUseCase,
    private val renameNoteUseCase: RenameNoteUseCase,
    private val moveNoteUseCase: MoveNoteUseCase,
    private val saveNoteContentUseCase: SaveNoteContentUseCase,
    private val shareNoteFileUseCase: ShareNoteFileUseCase,
    private val updateNoteDateUseCase: UpdateNoteDateUseCase,
    private val noteId: String?,
    private val notebookPath: String?,
    private val searchQuery: String?,
    contentPosition: Int,
    private val unlockNotebookUseCase: UnlockNotebookUseCase,
    private val settingsInteractor: SettingsInteractor,
    private val isNotebookProtectedUseCase: IsNotebookProtectedUseCase,
    private val saveRecentNoteUseCase: SaveRecentNoteUseCase,
    private val removeRecentNoteUseCase: RemoveRecentNoteUseCase,
    private val getNotebooksUseCase: GetNotebooksUseCase,
) : ViewModel() {

    private val _noteEditState = MutableLiveData<NoteEditState>()
    val noteEditState: LiveData<NoteEditState> = _noteEditState

    private val _navigationEvent = MutableLiveData<NoteEditNavigationEvent?>()
    val navigationEvent: LiveData<NoteEditNavigationEvent?> = _navigationEvent

    private var currentNote: Note? = null
    fun getNote() = currentNote

    private val _isDateUpdating = MutableStateFlow(false)
    private var pendingSaveContent: String? = null
    private var currentScrollPosition: Int? = null
    private var notebookList: List<Notebook> = emptyList()

    private var currentSearchQuery: String? = searchQuery
    private var startContentPosition: Int? = contentPosition

    init {
        viewModelScope.launch { loadNote() }
        loadNotebooks()
    }

    fun getEncryptionStatus(): Boolean {
        return if (notebookPath.isNullOrBlank()) false else isNotebookProtectedUseCase(notebookPath)
    }

    private fun loadNotebooks() {
        viewModelScope.launch {
            try {
                notebookList = getNotebooksUseCase()
            } catch (e: Exception) {
                println("DEBUG: NoteEditVm: loadNotebooks: Error loading notebooks: ${e.message}")
            }
        }
    }

    fun getAllNotebooks(): List<Notebook> = notebookList

    fun reloadNotePosition() {
        val note = currentNote ?: return
        if (_noteEditState.value !is NoteEditState.Success) return

        val state = _noteEditState.value as NoteEditState.Success
        val searchState = state.searchState

        if (searchState == null)
            postNote(note)
        else
            postNoteAndSearch(searchState.query, searchState.matches, searchState.currentMatchIndex)
    }

    private suspend fun loadNote() {
        if (noteId != null) {
            _noteEditState.postValue(NoteEditState.Loading)
            println("DEBUG: NoteEditVM: Loading note id=$noteId path=$notebookPath")
            try {
                val note = getNoteUseCase(noteId, notebookPath)
                currentNote = note
                if (currentScrollPosition == null) currentScrollPosition = getNoteScrollPosition()
                println("DEBUG: NoteEditVM: Note loaded: ${note.printDebug()}. scroll=$currentScrollPosition")

                if (searchQuery == null)
                    postNote(note)
                else
                    performSearch(searchQuery)

                if (note.isNotEmpty()) removeRecentNoteUseCase(note)

            } catch (e: AuthenticationRequiredException) {
                _navigationEvent.postValue(NoteEditNavigationEvent.ShowBiometric)
                println("DEBUG: NoteEditVM: Key not unlocked while loading note: ${e.message}")

            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error loading: ${e.message}")
                _noteEditState.postValue(NoteEditState.Error(e.message ?: "Ошибка загрузки"))
            }
        }
    }

    fun onSearchQuerySubmitted(query: String) {
        println("DEBUG: NoteEditVM: search with query=$query")
        if (!query.isEmpty()) { //>=3
            performSearch(query)
        }
    }

    fun onSearchCleared() {
        currentSearchQuery = null
        val note = currentNote ?: return
        postNote(note)
    }

    // Навигация по совпадениям
    fun nextMatch() {
        val state = _noteEditState.value
        if (state is NoteEditState.Success && state.searchState != null) {
            val searchState = state.searchState
            if (searchState.matches.isNotEmpty()) {
                val newIndex = (searchState.currentMatchIndex + 1) % searchState.matches.size
                postNoteAndSearch(searchState.query, searchState.matches, newIndex)
            }
        }
    }

    fun previousMatch() {
        val state = _noteEditState.value
        if (state is NoteEditState.Success && state.searchState != null) {
            val searchState = state.searchState
            if (searchState.matches.isNotEmpty()) {
                val newIndex = if (searchState.currentMatchIndex - 1 < 0)
                    searchState.matches.size - 1
                else
                    searchState.currentMatchIndex - 1
                postNoteAndSearch(searchState.query, searchState.matches, newIndex)
            }
        }
    }

    private fun performSearch(query: String) {
        println("DEBUG: NoteEditVM: perform search, query=query, startContent=$startContentPosition")
        val note = currentNote ?: return

        if (note.title.isEmpty() && note.content.isEmpty()) {
            postNoteAndSearch(query, emptyList(), -1)
            return
        }
        currentSearchQuery = query
        val result = highlightNoteMatches(note.title, note.content, query, 0)

        val currentIndex = when {
            result.matches.isEmpty() -> -1

            startContentPosition == -1 -> 0

            (startContentPosition != null && startContentPosition!! >= 0) ->
                resolveStartIndex(result.matches, startContentPosition)

            else -> {
                // без курсора — по умолчанию встаём на первое совпадение в контенте,
                // если оно есть, иначе на первое вообще (в заголовке)
                val contentFirst = result.matches.indexOfFirst {
                    it.target == SearchMatchTarget.CONTENT
                }
                if (contentFirst >= 0) contentFirst else 0
            }
        }

        postNoteAndSearch(query, result.matches, currentIndex)
    }

    private fun resolveStartIndex(matches: List<SearchMatch>, cursorPosition: Int?): Int {
        if (cursorPosition == null) return -1
        //0. обнуляем начальное положение вхождения, чтобы оно не повлияло на последюущие поиски
        startContentPosition = null

        if (matches.isEmpty()) return -1

        if (cursorPosition == -1) return 0

        // 1. Точное совпадение начала матча в контенте — это основной сценарий.
        val exact = matches.indexOfFirst {
            it.target == SearchMatchTarget.CONTENT && it.start == cursorPosition
        }
        if (exact >= 0) return exact

        // 2. Курсор попал внутрь какого-то совпадения в контенте.
        val containing = matches.indexOfFirst {
            it.target == SearchMatchTarget.CONTENT &&
                    cursorPosition in it.start until it.end
        }
        if (containing >= 0) return containing

        // 3. Первое совпадение в контенте, начиная с позиции курсора.
        val nextFromCursor = matches.indexOfFirst {
            it.target == SearchMatchTarget.CONTENT && it.start >= cursorPosition
        }
        if (nextFromCursor >= 0) return nextFromCursor

        // 4. Совпадений в контенте правее нет — начинаем с первого совпадения в контенте.
        val firstContent = matches.indexOfFirst { it.target == SearchMatchTarget.CONTENT }
        if (firstContent >= 0) return firstContent

        // 5. Совпадений в контенте нет вообще — показываем первое (в заголовке).
        return 0
    }

    private fun postNoteAndSearch(query: String, matches: List<SearchMatch>, currentIndex: Int) {
        val note = currentNote ?: return
        println("DEBUG: NoteEditVM: postNoteAndSearch: matches:${matches.size}, currentIndex=$currentIndex")
        _noteEditState.postValue(
            NoteEditState.Success(
                note = note,
                scrollPosition = currentScrollPosition ?: 0,
                isEncrypted = getEncryptionStatus(),
                searchState = SearchState(query, matches, currentIndex)
            )
        )
    }

    fun lockNote() {
        _noteEditState.value = NoteEditState.Blocked(false)
    }

    fun updateNoteTitleIfChanged(newTitle: String) {
        val note = currentNote ?: return
        if (newTitle == note.title) return

        viewModelScope.launch {
            try {
                println("DEBUG: NoteEditVM: Updating note title to $newTitle, currentNote: ${currentNote?.printDebug()}")
                currentNote = renameNoteUseCase(note, newTitle)
                postNote(currentNote!!)
            } catch (e: Exception) {
                postNote(note)  //если название изменить не удалось, возвращаем прежнее
                showMessage("Ошибка при переименовании заметки: ${e.message}")
            }
        }
    }

    fun updateNoteContent(content: String) {
        println("DEBUG: NoteEditVM: Updating note content, current note: ${currentNote?.printDebug()}")
        val note = currentNote ?: return
        viewModelScope.launch {
            try {
                saveNoteContentUseCase(note.copy(content = content))
                currentNote = note.copy(content = content)
            } catch (e: AuthenticationRequiredException) {
                pendingSaveContent = content
                _noteEditState.postValue(NoteEditState.Blocked(false))
                println("DEBUG: NoteEditVM: key not unlocked while updating: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка при сохранении текста заметки: ${e.message}")
                println("DEBUG: NoteEditVM: error: ${e.message}")
            }
        }
    }

    fun unlockNote(context: Context) {
        if (currentNote == null)
            unlockAndLoad(context)
        else
            unlockAndSavePendingContent(context)
    }

    fun unlockAndLoad(context: Context) {
        viewModelScope.launch {
            val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                notebookPath, context, title = "Заметка защищена", reason = "Для разблокирования"
            ) else true

            if (unlocked) loadNote()
            else _noteEditState.postValue(NoteEditState.Blocked(false))
        }
    }

    fun unlockAndSavePendingContent(context: Context) {
        val note = currentNote ?: return
        println("DEBUG: NoteEditVM: unlockAndSavePendingContent: ${currentNote?.printDebug()}")

        viewModelScope.launch {
            try {
                val unlocked = if (notebookPath != null) unlockNotebookUseCase(
                    notebookPath, context, reason = "Для редактирования"
                ) else true

                if (unlocked) {
                    val newContent = pendingSaveContent

                    if (newContent != null) {
                        val updatedNote = note.copy(content = newContent)
                        saveNoteContentUseCase(updatedNote)
                        postNote(updatedNote)
                        currentNote = updatedNote
                        pendingSaveContent = null
                    } else {
                        postNote(note)
                    }
                } else {
                    _noteEditState.postValue(NoteEditState.Blocked(true))
                }
            } catch (e: AuthenticationRequiredException) {
                _noteEditState.postValue(NoteEditState.Blocked(true))
                println("DEBUG: NoteEditVM: key not unlocked while updating: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка при сохранении текста заметки: ${e.message}")
                println("DEBUG: NoteEditVM: unlockAndSavePendingContent: error: ${e.message}")
            }
        }
    }

    fun updateNoteDate(newDate: Long) {
        val note = currentNote ?: return

        viewModelScope.launch {
            _isDateUpdating.value = true

            try {
                updateNoteDateUseCase(note, newDate)

                currentNote = note.copy(modifiedAt = newDate)
                _noteEditState.postValue(
                    NoteEditState.Success(
                        note.copy(modifiedAt = newDate),
                        currentScrollPosition ?: 0,
                        isNotebookProtectedUseCase(note.notebookPath ?: "")
                    )
                )
                showMessage("Дата заметки изменена")
            } catch (e: Exception) {
                showMessage("Ошибка обновления даты: ${e.message}")
            } finally {
                _isDateUpdating.value = false
            }
        }
    }

    private fun postNote(note: Note) {
        _noteEditState.postValue(
            NoteEditState.Success(
                note,
                currentScrollPosition ?: 0,
                getEncryptionStatus(), null
            )
        )
    }

    private fun navigateBack() =
        _navigationEvent.postValue(NoteEditNavigationEvent.NavigateBack)

    fun saveToRecent() {
        val note = currentNote ?: return
        viewModelScope.launch {
            try {
                saveRecentNoteUseCase(note)
            } catch (e: Exception) {
                println("DEBUG: NoteEditVM: Error saving note to recent: ${e.message}")
            }
        }
    }

    fun shareNote(context: Context) {
        viewModelScope.launch {
            try {
                val unlocked = if (getEncryptionStatus())
                    unlockNotebookUseCase(notebookPath!!, context, reason = "Для экспорта")
                else true

                val note = currentNote ?: return@launch

                if (unlocked)
                    _navigationEvent.postValue(
                        NoteEditNavigationEvent.ShareNote(note)
                    )
                else _noteEditState.postValue(NoteEditState.Blocked(false))

            } catch (e: Exception) {
                showMessage("Ошибка при экспорте заметки: ${e.message}")
            }
        }
    }

    fun shareFile(context: Context) {
        val note = currentNote ?: return
        viewModelScope.launch {
            try {
                val unlocked = if (getEncryptionStatus())
                    unlockNotebookUseCase(notebookPath!!, context, reason = "Для экспорта")
                else true

                val file = shareNoteFileUseCase(note)

                if (unlocked)
                    _navigationEvent.postValue(
                        NoteEditNavigationEvent.ShareFile(file)
                    )
                else _noteEditState.postValue(NoteEditState.Blocked(false))

            } catch (e: Exception) {
                showMessage("Ошибка при экспорте заметки: ${e.message}")
            }
        }
    }

    fun moveNote(context: Context, targetNotebookPath: String) {
        val note = currentNote ?: return

        viewModelScope.launch {
            try {
                val unlocked =
                    if (isNotebookProtectedUseCase(targetNotebookPath)) unlockNotebookUseCase(
                        targetNotebookPath, context, title = "Целевая записная книжка защищена",
                        reason = "Для перемещения"
                    ) else true

                if (unlocked) {
                    moveNoteUseCase(note, targetNotebookPath)
                    navigateBack()
                } else {
                    showMessage("Не удалось разблокировать целевую записную книжку")
                }
            } catch (e: AuthenticationRequiredException) {
                showMessage("Не удалось разблокировать целевую записную книжку")
                println("DEBUG: NoteEditVM:  AuthenticationRequiredException ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка перемещения: ${e.message}")
            }
        }
    }

    fun deleteNote() {
        val note = currentNote ?: return

        viewModelScope.launch {
            try {
                deleteNoteUseCase(note)
                navigateBack()
            } catch (e: AuthenticationRequiredException) { //не может такого быть
                println("DEBUG: NoteEditVM: key not unlocked while deleting note: ${e.message}")
            } catch (e: Exception) {
                showMessage("Ошибка удаления: ${e.message}")
            }
        }
    }

    fun saveNoteScrollPosition(scrollPosition: Int) {
        if (noteId != null) {
            println("DEBUG: NoteEditVM: saveNoteScrollPosition: noteId=$noteId, notebookPath=$notebookPath, pos=$scrollPosition")
            settingsInteractor.saveNoteScrollPosition(noteId, notebookPath ?: "", scrollPosition)
        }
    }

    fun rememberNoteScrollPosition(scrollPosition: Int) {
        println("DEBUG: NoteEditVM: rememberNoteScrollPosition: noteId=$noteId, notebookPath=$notebookPath, pos=$scrollPosition")
        currentScrollPosition = scrollPosition
    }

    fun getNoteScrollPosition(): Int {
        return if (noteId != null) {
            settingsInteractor.getNoteScrollPosition(noteId, notebookPath ?: "") ?: 0
        } else 0
    }

    private fun showMessage(msg: String) =
        _navigationEvent.postValue(NoteEditNavigationEvent.ShowMessage(msg))

    fun clearEvent() {
        _navigationEvent.value = null
    }
}

