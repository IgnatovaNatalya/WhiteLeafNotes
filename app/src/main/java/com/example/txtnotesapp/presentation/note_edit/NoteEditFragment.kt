package com.example.txtnotesapp.presentation.note_edit

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.example.txtnotesapp.databinding.FragmentNoteEditBinding
import kotlinx.datetime.Instant
import kotlinx.datetime.toLocalDateTime


class NoteEditFragment : Fragment() {
    private lateinit var binding: FragmentNoteEditBinding
    private lateinit var viewModel: NoteEditViewModel

//    private val viewModel: NoteEditViewModel by viewModel{
//        parametersOf(requireArguments().getString("note_id"))
//    }

    private val args: NoteEditFragmentArgs by navArgs()

    private var isEditing = false
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        override fun afterTextChanged(s: Editable?) {
            if (isEditing) {
                viewModel.updateNoteContent(s.toString())
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNoteEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получение аргументов
        val noteId = args.noteTitle
        val notebookPath = args.notebookPath

        // Инициализация ViewModel
        viewModel = ViewModelProvider(
            this,
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return NoteEditViewModel(
                        get(),
                        get(),
                        get(),
                        noteId,
                        notebookPath
                    ) as T
                }
            }
        ).get(NoteEditViewModel::class.java)

        setupObservers()
        setupToolbar()
        setupEditText()
    }

    private fun setupObservers() {
        viewModel.note.observe(viewLifecycleOwner) { note ->
            binding.noteTitle.setText(note.title)
            binding.noteDate.text = formatDate(note.createdAt)

            // Устанавливаем текст только если он отличается от текущего
            if (binding.noteText.text.toString() != note.content) {
                isEditing = false
                binding.noteText.setText(note.content)
                isEditing = true
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            //binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.isSaved.observe(viewLifecycleOwner) { isSaved ->
            if (isSaved) {
                // todo сделать  индикатор сохранения
//                binding.saveIndicator.visibility = View.VISIBLE
//                Handler(Looper.getMainLooper()).postDelayed({
//                    binding.saveIndicator.visibility = View.GONE
//                }, 1000)
            }
        }
    }

    private fun setupToolbar() {
        binding.noteEditToolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        binding.noteEditToolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
//                R.id.menu_undo -> {
//                     TODO: Реализовать отмену
//                    true
//                }
//                R.id.menu_redo -> {
//                    TODO: Реализовать повтор
//                    true
//                }
//                R.id.menu_more -> {
//                    TODO: Показать дополнительное меню
//                    true
//                }
                else -> false
            }
        }
    }

    private fun setupEditText() {
        binding.noteText.addTextChangedListener(textWatcher)
        isEditing = true
    }

    override fun onPause() {
        super.onPause()
        // Принудительное сохранение при выходе с экрана
        viewModel.updateNoteContent(binding.noteText.text.toString())
    }

    private fun formatDate(timestamp: Long): String {
        val date = Instant.fromEpochMilliseconds(timestamp)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        return "${date.dayOfMonth} ${getMonthName(date.month)} ${date.year}"
    }

    private fun getMonthName(month: Month): String {
        return when (month) {
            Month.JANUARY -> "Января"
            Month.FEBRUARY -> "Февраля"
            Month.MARCH -> "Марта"
            Month.APRIL -> "Апреля"
            Month.MAY -> "Мая"
            Month.JUNE -> "Июня"
            Month.JULY -> "Июля"
            Month.AUGUST -> "Августа"
            Month.SEPTEMBER -> "Сентября"
            Month.OCTOBER -> "Октября"
            Month.NOVEMBER -> "Ноября"
            Month.DECEMBER -> "Декабря"
        }
    }
}