package ru.whiteleaf.notes.presentation.note_edit


import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import kotlinx.coroutines.launch
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
import ru.whiteleaf.notes.presentation.note_list.NoteListFragmentDirections

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>() {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath)
    }

    private val args: NoteEditFragmentArgs by navArgs()

    private var isEditing = false
    private var notSaveOnPause = false
    private var wasInterrupted = false
    private var isSaving = false

    private var lastCursorPosition = -1 //-1 если не была открыта клавиатура и не вводился текст

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var noteScrollView: NestedScrollView
    private lateinit var noteBlocked: LinearLayout
    private lateinit var noteBlockedUnsaved: LinearLayout
    private lateinit var btnLockIndicator: ImageButton
    private lateinit var progressBar: ProgressBar

    private var windowFocusListener: ViewTreeObserver.OnWindowFocusChangeListener? = null

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
        btnLockIndicator =
            (requireActivity() as AppCompatActivity).findViewById(R.id.btn_lock_indicator)
        progressBar = binding.noteEditProgressBar


        val backCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                println("DEBUG: NoteEditFragment: OnBackPressedCallback called")
                if (isSaving) return // уже сохраняем – игнорируем повторные нажатия
                performSaveAndExit()
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, backCallback)

        setupWindowFocusChangeListener(view)
        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
        setupScrollDown()
        setupClickListeners()
    }

    fun performSaveAndExit() {
        println("DEBUG: NoteEditFragment: performSaveAndExit isSaving=$isSaving")
        notSaveOnPause = true
        isSaving = true

        viewLifecycleOwner.lifecycleScope.launch {
            saveScrollPosition()
            viewModel.saveToRecent()

            viewModel.updateFullNote(
                titleEditText.text.toString(),
                contentEditText.text.toString(),
                onExit = true
            )
            notSaveOnPause = false
            isSaving = false
        }
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
            //println("DEBUG: NoteEditFragment: Window focus gone,saving scroll")

//            viewModel.updateFullNote(
//                titleEditText.text.toString(),
//                contentEditText.text.toString(),
//                false
//            )
            viewModel.updateNoteTitleIfChanged(titleEditText.text.toString())
            viewModel.rememberNoteScrollPosition(noteScrollView.scrollY)
            wasInterrupted = true

            if (checkKeyboard(contentEditText))
                lastCursorPosition = contentEditText.selectionStart
            //println("DEBUG: NoteEditFragment: Saving cursorPosition $lastCursorPosition")

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
        binding.noteEditDate.setOnClickListener { changeNoteDate() }

        binding.unlockButton.setOnClickListener {
            viewModel.unlockAndSavePendingContent(requireContext())
        }
        binding.unlockAndSaveButton.setOnClickListener {
            viewModel.unlockAndSavePending(requireContext(), false)
        }

        binding.cancelButton.setOnClickListener { findNavController().popBackStack() }
    }

    fun changeNoteDate() {
        viewModel.updateFullNote(
            title = titleEditText.text.toString(),
            content = contentEditText.text.toString(),
            onExit = false
        )
        viewModel.rememberNoteScrollPosition(noteScrollView.scrollY)
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

        (requireContext().getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(titleEditText, InputMethodManager.SHOW_IMPLICIT)
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
        if (noteIsNotEmpty()) {
            viewModel.unlockAndFullSave(
                requireContext(),
                titleEditText.text.toString(),
                contentEditText.text.toString()
            )
            viewModel.shareNoteFile()
        } else renderMessage("Пустая заметка")
    }

    private fun noteIsNotEmpty() =
        titleEditText.text.toString().trim() != "" || contentEditText.text.toString().trim() != ""

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
                println("DEBUG: NoteEditFragment: Rendering note")
                noteScrollView.visibility = View.VISIBLE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                toggleSecurePreview(state.isEncrypted)

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

                if (contentEditText.text.toString() != note.content) {
                    isEditing = false
                    contentEditText.setText(note.content)
                    isEditing = true
                }
                noteScrollView.post { noteScrollView.scrollTo(0, state.scrollPosition) }
                //println("DEBUG: NoteEditFragment: Restore scroll position ${state.scrollPosition}")

                if (lastCursorPosition > 0) {
                    contentEditText.post {
                        println("DEBUG: NoteEditFragment: Restore cursor $lastCursorPosition & showKeyboard ")
                        titleEditText.requestFocus()
                        contentEditText.requestFocus()
                        showKeyboard()
                        if (contentEditText.text.length >= lastCursorPosition) contentEditText.setSelection(
                            lastCursorPosition
                        ) else contentEditText.setSelection(contentEditText.text.length)
                        lastCursorPosition = -1
                    }
                }
            }

            NoteEditState.Loading -> {
                println("DEBUG: NoteEditFragment: Rendering loading")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.VISIBLE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
            }

            is NoteEditState.Error -> {
                println("DEBUG: NoteEditFragment: Rendering error")
                noteScrollView.visibility = View.GONE
                progressBar.visibility = View.GONE
                noteBlocked.visibility = View.GONE
                noteBlockedUnsaved.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
                renderMessage(state.message)
            }

            is NoteEditState.Blocked -> {
                println("DEBUG: NoteEditFragment: Rendering blocked hasChanges=${state.hasUnsavedChanges}")
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
                buttonScroll.visibility = View.GONE
            }
        }
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

            is NoteEditNavigationEvent.ShareFile -> ShareHelper.shareFile(
                requireContext(),
                event.uri
            )

            is NoteEditNavigationEvent.ShowBiometric -> {
                println("DEBUG: NoteEditFragment: Rendering ShowBiometricForSave OnExit=${event.onExit}")
                viewModel.unlockAndSavePending(requireContext(), event.onExit)
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ShowMessage -> {
                renderMessage(event.message)
                viewModel.clearEvent()
            }

            is NoteEditNavigationEvent.ReopenNote -> {
                val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
                    noteId = event.newId,
                    notebookPath = args.notebookPath
                )
                val navOptions = NavOptions.Builder()
                    .setPopUpTo(R.id.noteEditFragment, inclusive = true)
                    .build()
                findNavController().navigate(action, navOptions)
            }

            null -> {}
        }

    }

    private fun renderMessage(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun saveScrollPosition() {
        val lastScrollPosition = noteScrollView.scrollY
        viewModel.saveNoteScrollPosition(lastScrollPosition)
        println("DEBUG: NoteEditFragment: Scroll saved to prefs: $lastScrollPosition")
    }

    fun toggleSecurePreview(isSecure: Boolean) {
        println("DEBUG: toggleSecurePreview isSecure: $isSecure")
        if (isSecure) {
            requireActivity().window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )
        } else {
            requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }

    override fun onPause() {
        super.onPause()

        if (!notSaveOnPause) {
            //viewModel.updateNoteContent(contentEditText.text.toString())
            //saveScrollPosition()
            viewModel.rememberNoteScrollPosition(noteScrollView.scrollY)
            viewModel.saveToRecent()
            //println("Debug: NoteEditFragment: Saved content on pause")
        } else println("Debug: NoteEditFragment: Paused and not saved")
    }

    fun checkKeyboard(editText: EditText): Boolean {
        val imm =
            editText.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        return imm.isAcceptingText
    }

    fun showKeyboard() {
        val imm =
            contentEditText.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(contentEditText, InputMethodManager.SHOW_IMPLICIT)
    }

    override fun onDestroyView() {
        clearListeners()
        super.onDestroyView()
    }

    private fun clearListeners() {
        // 1. Удаляем слушатель с optionsButton (из Activity)
        //val optionsButton = requireActivity().findViewById<ImageButton>(R.id.btn_options_menu)
        //optionsButton?.setOnClickListener(null)

        // 2. Удаляем слушатель с btnLockIndicator (из Activity)
        //btnLockIndicator.setOnClickListener(null)

        // 3. Удаляем OnWindowFocusChangeListener
        windowFocusListener?.let {
            binding.root.viewTreeObserver.removeOnWindowFocusChangeListener(it)
        }
        windowFocusListener = null
    }
}