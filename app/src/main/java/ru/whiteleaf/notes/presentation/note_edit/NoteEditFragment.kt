package ru.whiteleaf.notes.presentation.note_edit

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
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.common.utils.TextWatcherManager
import ru.whiteleaf.notes.databinding.FragmentNoteEditBinding
import ru.whiteleaf.notes.domain.model.Note
import kotlin.time.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.whiteleaf.notes.common.utils.TextWatcherScrollManager
import kotlin.time.ExperimentalTime

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>() {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath)
    }

    private val args: NoteEditFragmentArgs by navArgs()
    private var isEditing = false
    private var isMoved = false
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var buttonScroll: ImageButton
    private lateinit var scrollView: NestedScrollView

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
        scrollView = binding.scrollView

        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
        setupScrollDown()
    }

    private fun setupObservers() {
        viewModel.note.observe(viewLifecycleOwner) { note ->
            binding.noteTitle.setText(note.title)
            binding.noteDate.text = formatDate(note.modifiedAt)

            if (binding.noteText.text.toString() != note.content) {
                isEditing = false
                binding.noteText.setText(note.content)
                isEditing = true
            }
        }

        viewModel.noteFile.observe(viewLifecycleOwner) { noteFile ->
            ShareHelper.shareFile(requireContext(), noteFile)
        }

        viewModel.noteMoved.observe(viewLifecycleOwner) {
            isMoved = true
            findNavController().navigateUp()
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }
    }

    private fun setupScrollDown() {
        TextWatcherScrollManager.setupScrollDetection(
            editText = contentEditText,
            scrollView = scrollView, // ваш NestedScrollView
            button = buttonScroll
        )
    }

    private fun setupEditTexts() {
        TextWatcherManager.setupEditText(
            editText = binding.noteText,
            condition = { isEditing },
            onAfterTextChanged = { text -> viewModel.updateNoteContent(text) }
        )

        binding.noteTitle.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            if (!hasFocus) {
                val textInput = binding.noteTitle.text.toString()
                viewModel.updateNoteTitle(textInput)
            }
        }
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

    override fun onPause() {
        if (!isMoved) {
            viewModel.updateFullNote(
                binding.noteTitle.text.toString(),
                binding.noteText.text.toString()
            )
        }
        super.onPause()
    }

    @OptIn(ExperimentalTime::class)
    private fun formatDate(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return "${date.day} ${getMonthName(date.month)} ${date.year}"
    }

    private fun getMonthName(month: Month): String {
        return when (month) {
            Month.JANUARY -> "января"
            Month.FEBRUARY -> "февраля"
            Month.MARCH -> "марта"
            Month.APRIL -> "апреля"
            Month.MAY -> "мая"
            Month.JUNE -> "июня"
            Month.JULY -> "июля"
            Month.AUGUST -> "августа"
            Month.SEPTEMBER -> "сентября"
            Month.OCTOBER -> "октября"
            Month.NOVEMBER -> "ноября"
            Month.DECEMBER -> "декабря"
        }
    }
}