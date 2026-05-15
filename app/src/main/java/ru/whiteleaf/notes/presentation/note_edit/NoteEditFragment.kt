package ru.whiteleaf.notes.presentation.note_edit

import android.app.DatePickerDialog
import android.content.Context.INPUT_METHOD_SERVICE
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
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
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.whiteleaf.notes.common.utils.TextWatcherScrollManager
import ru.whiteleaf.notes.common.utils.DateHelper
import java.util.Calendar
import kotlin.time.ExperimentalTime

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>() {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath)
    }

    private val args: NoteEditFragmentArgs by navArgs()

    private var isEditing = false
    private var isMoved = false
    private var wasInterruptedByNotification = false

    //private var lastScrollPosition = 0
    private var scrollRestoreRunnable: Runnable? = null

    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var scrollView: NestedScrollView

    // private val handler = Handler(Looper.getMainLooper())

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
        scrollView = binding.noteEditScrollView

        setupWindowFocusChangeListener(view)
        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
        setupScrollDown()
        setupClickListeners()
    }

    private fun setupWindowFocusChangeListener(view: View) {
        view.viewTreeObserver.addOnWindowFocusChangeListener { hasFocus ->
            if (!hasFocus) {
                //при переходе фокуса, клавиатура скрывается системой
                saveScrollPosition()
                wasInterruptedByNotification = true
                println("DEBUG: Window focus gone")

            } else if (wasInterruptedByNotification) {

                wasInterruptedByNotification = false
//                titleEditText.requestFocus()
//                titleEditText.setSelection(0)

                println("DEBUG: Window focus recieved")
                // убрали фокус на заголовок, пользователь сам нажмет на текст и клавиатура появится

                //restoreScrollPosition()
                viewModel.refreshNote()
            }
        }
    }

    private fun setupObservers() {
        viewModel.noteEditState.observe(viewLifecycleOwner) { state ->
            renderNote(state)
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
            scrollView = scrollView,
            //scrollView = binding.noteEditScrollView,
            button = buttonScroll
        )
    }

    private fun setupEditTexts() {
        TextWatcherManager.setupEditText(
            editText = binding.noteText,
            condition = { isEditing },
            onAfterTextChanged = { text -> viewModel.updateNoteContent(text) }
        )

        titleEditText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val textInput = binding.noteTitle.text.toString()
                viewModel.updateNoteTitle(textInput)
            }
        }
    }

    private fun setupClickListeners() {
        binding.noteDate.setOnClickListener {
            showMaterialDatePickerDialog()
            //showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val currentNote = viewModel.note.value ?: return

        // Получаем текущую дату из заметки
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentNote.modifiedAt
        }

        // Создаем DatePickerDialog
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            R.style.CustomDatePickerDialog,
            { _, year, month, dayOfMonth ->
                // Пользователь выбрал дату
                val selectedCalendar = Calendar.getInstance().apply {
                    set(
                        year,
                        month,
                        dayOfMonth,
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE)
                    )
                }
                viewModel.updateNoteDate(selectedCalendar.timeInMillis)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Дополнительные настройки диалога
        datePickerDialog.setTitle("Установите дату заметки")

        // Ограничения (опционально)
        // datePickerDialog.datePicker.maxDate = System.currentTimeMillis() // нельзя выбрать будущее

        datePickerDialog.show()
    }

    private fun showMaterialDatePickerDialog() {
        val currentNote = viewModel.note.value ?: return

        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentNote.modifiedAt
        }

        val datePicker = MaterialDatePicker.Builder.datePicker()
            //.setTheme(R.style.CustomMaterialCalendarTheme)
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
                binding.progressBar.visibility = View.GONE
                val note = state.note
                binding.noteTitle.setText(note.title)
                binding.noteDate.text = formatDate(note.modifiedAt)

                if (binding.noteText.text.toString() != note.content) {
                    isEditing = false
                    binding.noteText.setText(note.content)
                    isEditing = true
                }
                restoreScrollPosition()
            }

            NoteEditState.Loading -> {
                binding.progressBar.visibility = View.VISIBLE
            }

            is NoteEditState.Error -> {
                binding.progressBar.visibility = View.GONE
                renderMessage(state.message)
            }
        }
    }

    private fun renderMessage(msg: String) =
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()

    private fun saveScrollPosition() {
        val lastScrollPosition = scrollView.scrollY
        //val lastScrollPosition = binding.noteEditScrollView.scrollY
        viewModel.saveNoteScrollPosition(lastScrollPosition)
        println("DEBUG: Scroll saved to prefs: $lastScrollPosition")
    }

    private fun restoreScrollPosition() {
        // scrollRestoreRunnable?.let { scrollView.removeCallbacks(it) }

        val lastScrollPosition = viewModel.getNoteScrollPosition()
        scrollView.post { binding.noteEditScrollView.scrollTo(0, lastScrollPosition) }
        //binding.noteEditScrollView.scrollTo(0, lastScrollPosition)
        // scrollView.scrollTo(0, lastScrollPosition)
        println("DEBUG: Restore scroll position $lastScrollPosition")

//        scrollRestoreRunnable = Runnable {
//            scrollView.scrollTo(0, lastScrollPosition)
//            println("DEBUG: Set scroll position $lastScrollPosition")
//            //scrollRestoreRunnable = null
//        }
//        scrollView.post(scrollRestoreRunnable!!)
    }


    override fun onResume() {
        super.onResume()
        // При каждом входе на экран обновляем состояние безопасности
        viewModel.refreshNote()
    }

    override fun onPause() {
        saveScrollPosition()

        if (!isMoved) {
            viewModel.updateFullNote(
                binding.noteTitle.text.toString(),
                binding.noteText.text.toString()
            )
        }
        super.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Финальное сохранение и шифрование
        viewModel.saveAndEncryptOnExit()
    }

//    override fun onDestroyView() {
//        //удаляем вызов восстановления скролла
////        scrollRestoreRunnable?.let { binding.scrollView.removeCallbacks(it) }
////        scrollRestoreRunnable = null
//        super.onDestroyView()
//    }

//    override fun onStop() {
//        saveScrollPosition()
//        super.onStop()
//    }

    @OptIn(ExperimentalTime::class)
    private fun formatDate(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return "${date.day} ${DateHelper.getMonthName(date.month)} ${date.year}"
    }
}