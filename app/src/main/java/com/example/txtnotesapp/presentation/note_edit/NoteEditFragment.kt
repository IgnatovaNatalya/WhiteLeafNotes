package com.example.txtnotesapp.presentation.note_edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.txtnotesapp.R
import com.example.txtnotesapp.common.classes.BindingFragment
import com.example.txtnotesapp.common.utils.DialogHelper
import com.example.txtnotesapp.common.utils.ShareHelper
import com.example.txtnotesapp.common.utils.TextWatcherManager
import com.example.txtnotesapp.databinding.FragmentNoteEditBinding
import com.example.txtnotesapp.domain.model.Note
import kotlin.time.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.ExperimentalTime

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>() {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath)
    }

    private val args: NoteEditFragmentArgs by navArgs()
    private var isEditing = false
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private var isMoved = false

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

        setupOptionsMenu()
        setupObservers()
        setupEditTexts()
    }

    private fun setupObservers() {
        viewModel.note.observe(viewLifecycleOwner) { note ->
            binding.noteTitle.setText(note.title)
            binding.noteDate.text = formatDate(note.modifiedAt)

            // Устанавливаем текст только если он отличается от текущего
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
        viewModel.isSaved.observe(viewLifecycleOwner) { isSaved ->
            if (isSaved) {
                // todo сделать индикатор сохранения
//                binding.saveIndicator.visibility = View.VISIBLE
//                Handler(Looper.getMainLooper()).postDelayed({
//                    binding.saveIndicator.visibility = View.GONE
//                }, 1000)
            }
        }
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
        val menuProvider = object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.menu_edit, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                when (menuItem.itemId) {
                    android.R.id.home -> {}
                    R.id.options_undo -> {}
                    R.id.options_redo -> {}
                    R.id.options_rename_note -> {
                        Toast.makeText(requireContext(), "Переименовать", Toast.LENGTH_SHORT).show()
                        titleEditText.requestFocus()
                    }

                    R.id.options_move_note -> {
                        DialogHelper.createMoveNoteDialog(requireContext()) { newNotebookName ->
                            viewModel.moveNote(newNotebookName)
                        }.show()
                    }

                    R.id.options_share_note -> {
                        ShareHelper.shareNote(
                            requireContext(), Note(
                                id = titleEditText.text.toString(),
                                title = titleEditText.text.toString(),
                                content = contentEditText.text.toString(),
                                modifiedAt = System.currentTimeMillis(),
                                notebookPath = null,
                            )
                        )
                    }

                    R.id.options_share_note_file -> {
                        viewModel.updateFullNote(
                            titleEditText.text.toString(),
                            contentEditText.text.toString()
                        )
                        viewModel.shareNoteFile()
                    }

                    R.id.options_delete_note -> {}
                }
                return false
            }
        }
        requireActivity().addMenuProvider(menuProvider, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    override fun onPause() {
        if (!isMoved) {
            //Toast.makeText(requireContext(), "Save note pause", Toast.LENGTH_SHORT).show()
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