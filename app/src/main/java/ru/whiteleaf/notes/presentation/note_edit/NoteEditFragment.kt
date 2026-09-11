package ru.whiteleaf.notes.presentation.note_edit


import android.os.Bundle
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.common.utils.TextWatcherManager
import ru.whiteleaf.notes.databinding.FragmentNoteEditBinding
import ru.whiteleaf.notes.domain.model.Note
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.whiteleaf.notes.common.utils.DialogHelper.createChangeDateDialog
import ru.whiteleaf.notes.common.utils.TextWatcherScrollManager
import ru.whiteleaf.notes.common.utils.formatDate
import ru.whiteleaf.notes.common.utils.hideKeyboard
import ru.whiteleaf.notes.common.utils.highlightAllMatches
import ru.whiteleaf.notes.common.utils.showKeyboard
import ru.whiteleaf.notes.common.utils.toggleSecurePreview
import ru.whiteleaf.notes.presentation.root.RootActivity
import ru.whiteleaf.notes.presentation.search.SearchableFragment

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>(), SearchableFragment {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath, args.searchQuery)
    }

    private val args: NoteEditFragmentArgs by navArgs()

    private var isEditing = false
    private var isRenderingSearch = false
    private var notSaveOnPause = false
    private var wasInterrupted = false

    private var searchCursorPosition = -1
    private var searchQuery: String? = null

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var noteScrollView: NestedScrollView
    private lateinit var noteBlocked: LinearLayout
    private lateinit var noteBlockedUnsaved: LinearLayout
    private lateinit var btnLockIndicator: ImageButton
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
        noteBlocked = binding.llBlocked
        noteBlockedUnsaved = binding.llBlockedUnsaved
        progressBar = binding.noteEditProgressBar
        nextButton = binding.nextMatch
        prevButton = binding.prevMatch
        llMatchButtons = binding.llPrevNextButtons

        btnLockIndicator =
            (requireActivity() as AppCompatActivity).findViewById(R.id.btn_lock_indicator)

        searchQuery = args.searchQuery
        searchCursorPosition = args.contentPosition.takeIf { it != 0 } ?: -1

        highlightColor = ContextCompat.getColor(requireContext(), R.color.blue_transparent)

        setupSecurityPreview()
        setupWindowFocusChangeListener(view)
        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
        setupScrollDown()
        setupClickListeners()
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
            if (hasFocus && !isRenderingSearch) {
                clearHighlights()
                viewModel.onSearchCleared()
                (requireActivity() as RootActivity).searchClearFocus()
            }
        }

        titleEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val textInput = titleEditText.text.toString()
                viewModel.updateNoteTitleIfChanged(textInput)
            }
        }
    }

    private fun setupClickListeners() {
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
        val optionsButton = requireActivity().findViewById<ImageButton>(R.id.btn_options_menu)

        optionsButton?.setOnClickListener {
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
        if (noteIsNotEmpty()) ShareHelper.shareNote(
            requireContext(), Note(
                id = titleEditText.text.toString(),
                title = titleEditText.text.toString(),
                content = contentEditText.text.toString(),
                modifiedAt = System.currentTimeMillis(),
                notebookPath = null,
            )
        )
        else Toast.makeText(requireContext(), "Пустая заметка", Toast.LENGTH_SHORT).show()
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
                    //Результаты поиска
                    println("DEBUG: NoteEditFragment: rendering matches")
                    isRenderingSearch = true
                    isEditing = false
                    renderContentWithSearchResults(searchState)

                    if (searchQuery != null) {
                        val start = searchCursorPosition
                        val stop = searchCursorPosition + searchQuery!!.length
                        println("DEBUG: NoteEditFragment: setting selection first time $start to $stop")
                        contentEditText.requestFocus()
                        contentEditText.setSelection(start, stop)
                        searchQuery = null
                    }
                    isRenderingSearch = false

                } else {
                    //Обычный вид
                    println("DEBUG: NoteEditFragment: rendering note no search")
                    if (isEditing) showKeyboard(contentEditText)

                    if (contentEditText.text.toString() != note.content) { //не трогаем если уже заполняли
                        println("DEBUG: NoteEditFragment: set content")
                        isEditing = false        //не хотим чтоб сразу открылась клавиатура
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
                llMatchButtons.visibility = View.GONE
            }

            is NoteEditState.Error -> {
                println("DEBUG: NoteEditFragment: Rendering error")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
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
                llMatchButtons.visibility = View.GONE
            }
        }
    }

    private fun clearHighlights() {
        val text = viewModel.getNote()?.content ?: return
        contentEditText.setText(text)
    }

    private fun renderContentWithSearchResults(searchState: SearchState) {
        val content = viewModel.getNote()?.content ?: return

        val query = searchState.query
        val matches = searchState.matches
        val currentIndex = searchState.currentMatchIndex

        val (spannable, _) = highlightAllMatches(content, query, highlightColor)

        contentEditText.setText(spannable, TextView.BufferType.SPANNABLE)

        // Если есть совпадения, устанавливаем курсор на текущее
        if (matches.isNotEmpty() && currentIndex in matches.indices) {
            val (start, end) = matches[currentIndex]
            println("DEBUG: NoteEditFragment: Rendering matches currentIndex=$currentIndex start=$start end=$end")
            contentEditText.requestFocus()
            contentEditText.post { contentEditText.setSelection(start, end) }

            // Прокручиваем к видимости этого совпадения (опционально)
        }
        buttonScroll.visibility = View.GONE
        llMatchButtons.visibility = if (matches.size > 1) View.VISIBLE else View.GONE
        prevButton.isEnabled = currentIndex > 0
        nextButton.isEnabled = currentIndex < matches.lastIndex

    }

    private fun renderEvent(event: NoteEditNavigationEvent?) {
        println("DEBUG: NoteEditFragment: renderEvent: event=$event")
        when (event) {
            NoteEditNavigationEvent.NavigateBack -> {
                println("DEBUG: NoteEditFragment: NavigateBack")
                notSaveOnPause = true
                findNavController().popBackStack()
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShareFile ->
                ShareHelper.shareFile(requireContext(), event.uri)

            is NoteEditNavigationEvent.ShowMessage -> {
                renderMessage(event.message)
                viewModel.clearEvent()
            }

            NoteEditNavigationEvent.ShowBiometric -> {
                viewModel.unlockAndLoad(requireContext())
                viewModel.clearEvent()
            }

            null -> {}
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


//    override fun onDestroyView() {
//        clearListeners()
//        super.onDestroyView()
//    }

    //   private fun clearListeners() {
    // 1. Удаляем слушатель с optionsButton (из Activity)
    //val optionsButton = requireActivity().findViewById<ImageButton>(R.id.btn_options_menu)
    //optionsButton?.setOnClickListener(null)

    // 2. Удаляем слушатель с btnLockIndicator (из Activity)
    //btnLockIndicator.setOnClickListener(null)

    // 3. Удаляем OnWindowFocusChangeListener
//        windowFocusListener?.let {
//            binding.root.viewTreeObserver.removeOnWindowFocusChangeListener(it)
//        }
//        windowFocusListener = null
//    }
}