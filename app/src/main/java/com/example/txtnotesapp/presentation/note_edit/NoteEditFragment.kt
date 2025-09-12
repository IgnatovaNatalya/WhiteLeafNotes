package com.example.txtnotesapp.presentation.note_edit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.navArgs
import com.example.txtnotesapp.common.classes.BindingFragment
import com.example.txtnotesapp.common.utils.TextWatcherManager
import com.example.txtnotesapp.databinding.FragmentNoteEditBinding
import kotlinx.datetime.Instant
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NoteEditFragment : BindingFragment<FragmentNoteEditBinding>() {

    private val viewModel: NoteEditViewModel by viewModel {
        parametersOf(args.noteId, args.notebookPath)
    }

    private val args: NoteEditFragmentArgs by navArgs()
    private var isEditing = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNoteEditBinding {
        return FragmentNoteEditBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.notebookPath

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

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
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

    override fun onPause() {
        //Toast.makeText(requireContext(),"Save note pause",Toast.LENGTH_SHORT).show()
        viewModel.updateFullNote(
            binding.noteTitle.text.toString(),
            binding.noteText.text.toString()
        )
        super.onPause()
    }


    private fun formatDate(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return "${date.dayOfMonth} ${getMonthName(date.month)} ${date.year}"
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