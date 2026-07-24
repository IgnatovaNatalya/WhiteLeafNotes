package ru.whiteleaf.notes.presentation.note_edit


import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.android.material.datepicker.MaterialDatePicker
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
import ru.whiteleaf.notes.common.utils.TextWatcherScrollManager
import ru.whiteleaf.notes.common.utils.formatDate
import java.util.Calendar

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>() {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath)
    }

    private val args: NoteEditFragmentArgs by navArgs()

    private var isEditing = false
    private var isMoved = false
    private var wasInterrupted = false

    private var lastCursorPosition = -1 //-1 если не была открыта клавиатура и не вводился текст

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var noteScrollView: NestedScrollView
    private lateinit var noteProtected: LinearLayout
    private lateinit var btnLockIndicator: ImageButton

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNoteEditBinding {
        return FragmentNoteEditBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        titleEditText = binding.noteTitle
        contentEditText = binding.noteText
        buttonScroll = binding.noteScrollDown
        noteScrollView = binding.noteEditScrollView
        noteProtected = binding.noteInProtectedNotebook
        btnLockIndicator =
            (requireActivity() as AppCompatActivity).findViewById(R.id.btn_lock_indicator)

        //val actionBar = (requireActivity() as AppCompatActivity).supportActionBar
        //actionBar?.title = args.notebookPath

        setupWindowFocusChangeListener(view)
        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
        setupScrollDown()
        setupClickListeners()
    }


//    override fun onAttach(context: Context) {
//        super.onAttach(context)

//        val toolbar = (requireActivity() as AppCompatActivity).findViewById<Toolbar>(R.id.toolbar)
//        val path = args.notebookPath

//        if (!path.isNullOrBlank()) {
//            toolbar.setOnClickListener {
//                println("DEBUG: NoteEditFragment: notebook title ${path} clicked")
//                val action =
//                    NoteEditFragmentDirections.actionNoteEditFragmentToNoteListFragment(path)
//                findNavController().navigate(action)
//            }
//        }
 //   }

    private fun setupWindowFocusChangeListener(view: View) {
        view.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            onFocusChanged(hasFocus)
        }
    }

    fun onFocusChanged(hasFocus: Boolean) {
        if (!hasFocus) {
            //при переходе фокуса, клавиатура скрывается системой
            //println("DEBUG: NoteEditFragment: Window focus gone,saving scroll")
            saveScrollPosition()
            wasInterrupted = true

            if (checkKeyboard(contentEditText))
                lastCursorPosition = contentEditText.selectionStart
            //println("DEBUG: NoteEditFragment: Saving cursorPosition $lastCursorPosition")

        } else if (wasInterrupted) {
            wasInterrupted = false
            //println("DEBUG: NoteEditFragment: Window focus recieved")
            viewModel.refreshNote()
        }
    }

    private fun setupObservers() {
        viewModel.noteEditState.observe(viewLifecycleOwner) { state ->
            renderNote(state)
        }

        viewModel.isNotebookProtected.observe(viewLifecycleOwner) { isProtected ->
            toggleSecurePreview(isProtected)
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                renderMessage(message)
                viewModel.clearMessage()
            }
        }

        viewModel.noteFile.observe(viewLifecycleOwner) { noteFile ->
            ShareHelper.shareFile(requireContext(), noteFile)
        }

        viewModel.noteMoved.observe(viewLifecycleOwner) {
            isMoved = true
            findNavController().navigateUp()
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

        titleEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val textInput = titleEditText.text.toString()
                viewModel.updateNoteTitle(textInput)
            }
        }
    }

    private fun setupClickListeners() {
        binding.noteDate.setOnClickListener {
            saveScrollPosition()
            showMaterialDatePickerDialog()
        }

        binding.unlockButton.setOnClickListener {
            viewModel.unlockNotebook(requireContext())
        }
    }

    private fun showMaterialDatePickerDialog() {
        val currentNote = viewModel.note.value ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentNote.modifiedAt
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Выберите дату создания")
            .setSelection(calendar.timeInMillis)
            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
            .build()

        datePicker.addOnPositiveButtonClickListener { selection ->
            // selection - это Long с выбранной датой (в миллисекундах)
            // MaterialDatePicker возвращает дату в UTC, поэтому нужно учесть временную зону
            val selectedDate = selection // уже в миллисекундах

            // Сохраняем время из текущей заметки, но с новой датой
            val currentCalendar = Calendar.getInstance().apply {
                timeInMillis = currentNote.modifiedAt
            }

            val newCalendar = Calendar.getInstance().apply {
                timeInMillis = selectedDate
                set(Calendar.HOUR_OF_DAY, currentCalendar.get(Calendar.HOUR_OF_DAY))
                set(Calendar.MINUTE, currentCalendar.get(Calendar.MINUTE))
                set(Calendar.SECOND, currentCalendar.get(Calendar.SECOND))
            }

            viewModel.updateNoteDate(newCalendar.timeInMillis)
        }

        datePicker.show(childFragmentManager, "date_picker")
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

    private fun onOptionsMoveNote() {
        DialogHelper.createMoveNoteDialog(requireContext()) { newNotebookName ->
            viewModel.moveNote(newNotebookName)
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
            viewModel.updateFullNote(
                titleEditText.text.toString(),
                contentEditText.text.toString()
            )
            viewModel.shareNoteFile()
        } else Toast.makeText(requireContext(), "Пустая заметка", Toast.LENGTH_SHORT).show()
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

    private fun renderNote(state: NoteEditState) {
        when (state) {

            is NoteEditState.Success -> {
                println("DEBUG: NoteEditFragment: Rendering note")
                noteScrollView.visibility = View.VISIBLE
                binding.noteEditProgressBar.visibility = View.GONE
                noteProtected.visibility = View.GONE

                if (state.isEncrypted) {
                    btnLockIndicator.setImageResource(R.drawable.ic_ind_unlocked)
                    btnLockIndicator.visibility = View.VISIBLE
                    btnLockIndicator.setOnClickListener { viewModel.lockNote() }
                } else btnLockIndicator.visibility = View.GONE


                val note = state.note
                titleEditText.setText(note.title)

                binding.noteDate.text = formatDate(note.modifiedAt)

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
                        contentEditText.setSelection(lastCursorPosition)
                        lastCursorPosition = -1
                    }
                }
            }

            NoteEditState.Loading -> {
                println("DEBUG: NoteEditFragment: Rendering loading")
                noteScrollView.visibility = View.GONE
                binding.noteEditProgressBar.visibility = View.VISIBLE
                noteProtected.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
            }

            is NoteEditState.Error -> {
                println("DEBUG: NoteEditFragment: Rendering error")
                noteScrollView.visibility = View.GONE
                binding.noteEditProgressBar.visibility = View.GONE
                noteProtected.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
                renderMessage(state.message)
            }

            NoteEditState.Blocked -> {
                println("DEBUG: NoteEditFragment: Rendering blocked")
                binding.noteEditProgressBar.visibility = View.GONE
                noteProtected.visibility = View.VISIBLE
                noteScrollView.visibility = View.GONE
                btnLockIndicator.setImageResource(R.drawable.ic_ind_locked)
                btnLockIndicator.visibility = View.VISIBLE
            }
        }
    }

    private fun renderMessage(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun saveScrollPosition() {
        val lastScrollPosition = noteScrollView.scrollY
        viewModel.saveNoteScrollPosition(lastScrollPosition)
        //println("DEBUG: NoteEditFragment: Scroll saved to prefs: $lastScrollPosition")
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

        if (!isMoved) {
            viewModel.updateFullNote(
                titleEditText.text.toString(),
                contentEditText.text.toString()
            )
            saveScrollPosition()
            viewModel.saveToRecent()
        }
        println("Debug: NoteEditFragment: Paused")
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

}