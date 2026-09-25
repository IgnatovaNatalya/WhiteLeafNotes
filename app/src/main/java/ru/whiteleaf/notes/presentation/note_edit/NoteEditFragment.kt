package ru.whiteleaf.notes.presentation.note_edit


import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import org.koin.androidx.viewmodel.ext.android.activityViewModel
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.common.utils.TextWatcherManager
import ru.whiteleaf.notes.databinding.FragmentNoteEditBinding
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.whiteleaf.notes.common.utils.DialogHelper.createChangeDateDialog
import ru.whiteleaf.notes.common.utils.TextWatcherScrollManager
import ru.whiteleaf.notes.common.utils.formatDate
import ru.whiteleaf.notes.common.utils.hideKeyboard
import ru.whiteleaf.notes.common.utils.highlightNoteMatches
import ru.whiteleaf.notes.common.utils.showKeyboard
import ru.whiteleaf.notes.common.utils.toggleSecurePreview
import ru.whiteleaf.notes.presentation.root.RootActivity
import ru.whiteleaf.notes.presentation.root.RootViewModel
import ru.whiteleaf.notes.presentation.search.SearchableFragment
import kotlin.getValue

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>(), SearchableFragment {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath, args.searchQuery, args.contentPosition)
    }

    private val rootViewModel: RootViewModel by activityViewModel()

    private val args: NoteEditFragmentArgs by navArgs()

    private var isEditing = false
    private var notSaveOnPause = false
    private var wasInterrupted = false

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var noteScrollView: NestedScrollView
    private lateinit var noteBlocked: LinearLayout
    private lateinit var noteBlockedUnsaved: LinearLayout
    private lateinit var btnLockIndicator: ImageButton
    private lateinit var searchButton: SearchView
    private lateinit var optionsButton: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var nextButton: ImageButton
    private lateinit var prevButton: ImageButton
    private lateinit var llMatchButtons: LinearLayout

    private var windowFocusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null

    private var highlightColor = 0

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNoteEditBinding {
        return FragmentNoteEditBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleEditText = binding.noteEditTitle
        contentEditText = binding.noteEditText
        buttonScroll = binding.noteScrollDown
        noteScrollView = binding.noteEditScrollView
        noteBlocked = binding.llBlocked
        noteBlockedUnsaved = binding.llBlockedUnsaved
        progressBar = binding.noteEditProgressBar
        nextButton = binding.nextMatch
        prevButton = binding.prevMatch
        llMatchButtons = binding.llPrevNextButtons

        btnLockIndicator = requireActivity().findViewById(R.id.btn_lock_indicator)
        optionsButton = requireActivity().findViewById(R.id.btn_options_menu)
        searchButton = requireActivity().findViewById(R.id.search_view)

        highlightColor = ContextCompat.getColor(requireContext(), R.color.blue_transparent)

        if (args.searchQuery == null) (requireActivity() as RootActivity).cancelSearch()

        setupSecurityPreview()
        setupWindowFocusChangeListener(view)
        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
        setupScrollDown()
        setupClickListeners()
        setupBackCallback()
        setupSearchView()
    }

    private fun setupSearchView() {
        val searchView = (requireActivity() as RootActivity).findViewById<SearchView>(R.id.search_view)
        searchView.queryHint = "Поиск по заметке"
    }

    private fun setupBackCallback() {
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            println("DEBUG: NoteEditFragment: BackCallback: isSearching=${rootViewModel.isSearching()}, external search query=${args.searchQuery}")
            if (rootViewModel.isSearching()) {
                if (args.searchQuery != null) {
                    println("DEBUG: NoteEditFragment: navigated up")
                    findNavController().navigateUp()
                }
                else {
                    println("DEBUG: NoteEditFragment:search cancelled in RootVM")
                    (requireActivity() as RootActivity).cancelSearch()
                }
            } else {
                println("DEBUG: NoteEditFragment: navigated up")
                findNavController().navigateUp()
            }
        }
    }

    private fun setupSecurityPreview() {
        toggleSecurePreview(requireActivity(), viewModel.getEncryptionStatus())
    }

    private fun setupWindowFocusChangeListener(view: View) {
        windowFocusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            onFocusChanged(hasFocus)
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(windowFocusListener)
    }

    fun onFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            //при переходе фокуса, клавиатура скрывается системой

            viewModel.updateNoteTitleIfChanged(titleEditText.text.toString()) //обновляем только заголовок, т.к. контент и так сохраняется при любом изменении
            viewModel.rememberNoteScrollPosition(noteScrollView.scrollY)
            wasInterrupted = true
            contentEditText.clearFocus() //снимаем фокус с контента, чтобы клавитура могла включиться потом
        } else if (wasInterrupted) {
            wasInterrupted = false
            //println("DEBUG: NoteEditFragment: Window focus received")
            viewModel.reloadNotePosition()
        }
    }

    private fun setupObservers() {
        viewModel.noteEditState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            renderEvent(event)
        }
    }

    private fun setupScrollDown() {
        TextWatcherScrollManager.setupScrollDetection(
            editText = contentEditText,
            scrollView = noteScrollView,
            button = buttonScroll
        )
    }

    private fun setupEditTexts() {
        TextWatcherManager.setupEditText(
            editText = contentEditText,
            condition = { isEditing },
            onAfterTextChanged = { text -> viewModel.updateNoteContent(text) }
        )

        contentEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            isEditing = hasFocus
        }

        titleEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val textInput = titleEditText.text.toString()
                viewModel.updateNoteTitleIfChanged(textInput)
            }
        }
    }

    private fun setupClickListeners() {

        noteScrollView.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            viewModel.rememberNoteScrollPosition(scrollY)
        }

        binding.noteEditDate.setOnClickListener { changeNoteDate() }

        binding.unlockButton.setOnClickListener {
            viewModel.unlockNote(requireContext())
        }

        binding.unlockAndSaveButton.setOnClickListener {
            viewModel.unlockAndSavePendingContent(requireContext())
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }

        nextButton.setOnClickListener { viewModel.nextMatch() }
        prevButton.setOnClickListener { viewModel.previousMatch() }
    }

    override fun onSearchQueryChanged(query: String) {
        //ничего не делаем пока не нажмет сабмит
        contentEditText.clearFocus()
        if (query == "") clearHighlights()
    }

    override fun onSearchQuerySubmitted(query: String) {
        viewModel.onSearchQuerySubmitted(query)
        hideKeyboard(contentEditText)
    }

    override fun onSearchCleared() {
        clearHighlights()
        viewModel.onSearchCleared()
    }

    override fun onSearchStarted() {}

    fun changeNoteDate() {
        val note = viewModel.getNote() ?: return
        createChangeDateDialog(
            note.modifiedAt,
            onDateSelected = { date -> viewModel.updateNoteDate(date) }
        ).show(childFragmentManager, "date_picker")
    }

    private fun setupOptionsMenu() {

        optionsButton.setOnClickListener {
            titleEditText.clearFocus()
            contentEditText.clearFocus()
            ContextMenuHelper.showPopupMenu(
                context = requireContext(),
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuItemsNoteEdit(optionsButton.context),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_rename_note -> onOptionsRenameNote()
                        R.id.options_change_note_date -> onOptionsChangeNoteDate()
                        R.id.options_move_note -> onOptionsMoveNote()
                        R.id.options_share_note -> onOptionsShareNote()
                        R.id.options_share_note_file -> onOptionsShareNoteFile()
                        R.id.options_delete_note -> onOptionsDeleteNote()
                    }
                }
            )
        }
    }

    private fun onOptionsRenameNote() {
        titleEditText.requestFocus()
    }

    private fun onOptionsChangeNoteDate() = changeNoteDate()

    private fun onOptionsMoveNote() {
        DialogHelper.createMoveNoteDialog(
            requireContext(),
            viewModel.getAllNotebooks(),
            args.notebookPath ?: "",
        ) { targetNotebookPath ->
            viewModel.moveNote(requireContext(), targetNotebookPath)
        }.show()
    }

    private fun onOptionsShareNote() {
        viewModel.shareNote(requireContext())
    }

    private fun onOptionsShareNoteFile() {
        if (noteIsNotEmpty()) viewModel.shareFile(requireContext())
        else renderMessage("Пустая заметка")
    }

    private fun onOptionsDeleteNote() {
        DialogHelper.createDeleteNoteConfirmationDialog(
            requireContext(),
            titleEditText.text.toString()
        )
        { viewModel.deleteNote() }.show()
    }

    private fun renderState(state: NoteEditState) {
        when (state) {

            is NoteEditState.Success -> {
                val st =
                    if (state.searchState == null) "search state is null" else state.searchState.debugString()
                println("DEBUG: NoteEditFragment: Rendering note, $st, isEditing=$isEditing")
                noteScrollView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                searchButton.visibility = View.VISIBLE
                optionsButton.visibility = View.VISIBLE
                if (state.isEncrypted) {
                    btnLockIndicator.setImageResource(R.drawable.ic_ind_unlocked)
                    btnLockIndicator.visibility = View.VISIBLE
                    btnLockIndicator.setOnClickListener(null)
                    btnLockIndicator.setOnClickListener { viewModel.lockNote() }
                } else
                    btnLockIndicator.visibility = View.GONE

                val note = state.note
                titleEditText.setText(note.title)
                binding.noteEditDate.text = formatDate(note.modifiedAt)

                val searchState = state.searchState

                if (searchState != null) {
                    //результаты поиска
                    println("DEBUG: NoteEditFragment: rendering matches")
                    isEditing = false
                    renderSearchResults(searchState)
                } else {
                    //Обычный вид
                    println("DEBUG: NoteEditFragment: rendering note no search")
                    if (isEditing) showKeyboard(contentEditText)

                    if (contentEditText.text.toString() != note.content) { //не трогаем если уже заполняли
                        println("DEBUG: NoteEditFragment: set content, scroll pos = ${state.scrollPosition}")
                        isEditing = false                        //не хотим чтоб сразу открылась клавиатура
                        contentEditText.setText(note.content)    //заполняем контент если его нет
                    }
                    noteScrollView.post { noteScrollView.scrollTo(0, state.scrollPosition) }
                    llMatchButtons.visibility = View.GONE
                }
            }

            NoteEditState.Loading -> {
                println("DEBUG: NoteEditFragment: Rendering loading")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
                searchButton.visibility = View.GONE
                optionsButton.visibility = View.GONE
                llMatchButtons.visibility = View.GONE
            }

            is NoteEditState.Error -> {
                println("DEBUG: NoteEditFragment: Rendering error")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
                searchButton.visibility = View.GONE
                optionsButton.visibility = View.GONE
                buttonScroll.visibility = View.GONE
                renderMessage(state.message)
                llMatchButtons.visibility = View.GONE
            }

            is NoteEditState.Blocked -> {
                println("DEBUG: NoteEditFragment: Rendering blocked hasChanges=${state.hasUnsavedChanges}")
                hideKeyboard(contentEditText)

                progressBar.visibility = View.GONE

                if (state.hasUnsavedChanges) {
                    noteBlocked.visibility = View.GONE
                    noteBlockedUnsaved.visibility = View.VISIBLE
                } else {
                    noteBlocked.visibility = View.VISIBLE
                    noteBlockedUnsaved.visibility = View.GONE
                }
                noteScrollView.visibility = View.GONE
                btnLockIndicator.setImageResource(R.drawable.ic_ind_locked)
                btnLockIndicator.visibility = View.VISIBLE
                searchButton.visibility = View.GONE
                optionsButton.visibility = View.GONE
                llMatchButtons.visibility = View.GONE
            }
        }
    }

    private fun clearHighlights() {
        listOf(titleEditText, contentEditText).forEach { et ->
            val text = et.text as? Spannable ?: return@forEach
            text.getSpans(0, text.length, BackgroundColorSpan::class.java)
                .forEach { text.removeSpan(it) }
        }
    }

    private fun renderSearchResults(state: SearchState) {
        val note = viewModel.getNote() ?: return

        val result = highlightNoteMatches(
            title = note.title,
            content = note.content,
            query = state.query,
            highlightColor = highlightColor
        )

        titleEditText.setText(result.title, TextView.BufferType.SPANNABLE)
        contentEditText.setText(result.content, TextView.BufferType.SPANNABLE)

        val match = state.matches.getOrNull(state.currentMatchIndex)
        if (match != null) {
            val field = when (match.target) {
                SearchMatchTarget.TITLE -> titleEditText
                SearchMatchTarget.CONTENT -> contentEditText
            }
            field.requestFocus()
            field.post { field.setSelection(match.start, match.end) }
        }

        buttonScroll.visibility = View.GONE
        llMatchButtons.visibility = if (state.matches.size > 1) View.VISIBLE else View.GONE
        prevButton.isEnabled = state.currentMatchIndex > 0
        nextButton.isEnabled = state.currentMatchIndex < state.matches.lastIndex

    }

    private fun renderEvent(event: NoteEditNavigationEvent?) {
        println("DEBUG: NoteEditFragment: renderEvent: event=$event")
        if(event == null) return
        when (event) {
            NoteEditNavigationEvent.NavigateBack -> {
                println("DEBUG: NoteEditFragment: NavigateBack")
                notSaveOnPause = true
                findNavController().popBackStack()
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShareFile -> {
                ShareHelper.shareFile(requireContext(), event.uri)
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShareNote -> {
                ShareHelper.shareNote(requireContext(), event.note)
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShowMessage -> {
                renderMessage(event.message)
                viewModel.clearEvent()
            }

            NoteEditNavigationEvent.ShowBiometric -> {
                viewModel.unlockAndLoad(requireContext())
                viewModel.clearEvent()
            }
        }
    }

    private fun renderMessage(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun noteIsNotEmpty() =
        titleEditText.text.toString().trim() != "" || contentEditText.text.toString().trim() != ""

    override fun onPause() {
        super.onPause()

        if (!notSaveOnPause) {
            viewModel.updateNoteTitleIfChanged(titleEditText.text.toString())
            viewModel.saveNoteScrollPosition(noteScrollView.scrollY)
            viewModel.saveToRecent()
            println("Debug: NoteEditFragment: Saved scroll and recent on pause title updated(if changed)")
        } else println("Debug: NoteEditFragment: Paused and not saved")
    }


    override fun onDestroyView() {
        clearListeners()
        super.onDestroyView()
    }

    private fun clearListeners() {
        windowFocusListener?.let {
            binding.root.viewTreeObserver.removeOnWindowFocusChangeListener(it)
        }
        windowFocusListener = null
    }
}