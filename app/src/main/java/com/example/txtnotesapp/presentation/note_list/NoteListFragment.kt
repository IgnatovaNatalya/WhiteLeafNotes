package com.example.txtnotesapp.presentation.note_list

import androidx.appcompat.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.FragmentNoteListBinding
import com.example.txtnotesapp.domain.model.Note
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf

class NoteListFragment : Fragment() {
    private lateinit var binding: FragmentNoteListBinding

    private val viewModel: NoteListViewModel by viewModel { parametersOf(args.notebookPath) }
    private val args: NoteListFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()
        setupRecyclerView()
        setupFab()
    }

    private fun setupObservers() {
        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            toggleEmptyState(notes.isEmpty())
            (binding.recyclerView.adapter as NoteAdapter).submitList(notes)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }

        viewModel.navigateToNote.observe(viewLifecycleOwner) { noteId ->
            noteId?.let {
                navigateToNoteEdit(noteId)
                viewModel.onNoteNavigated()
            }
        }
    }

    private fun setupRecyclerView() {
        val adapter = NoteAdapter(
            onNoteClicked = { note ->
                viewModel.onNoteClicked(note.title)
            },
            onNoteLongClicked = { note ->
                showContextMenu(note)
            }
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Добавляем разделитель между элементами
        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupFab() {
        binding.createNote.setOnClickListener {
            viewModel.createNewNote()
        }
    }

    private fun navigateToNoteEdit(noteTitle: String) {
        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditFragment(
            noteTitle = noteTitle,
            notebookPath = args.notebookPath
        )
        findNavController().navigate(action)
    }

    private fun showContextMenu(note: Note) {
        val popup = PopupMenu(requireContext(), binding.root)
        popup.menuInflater.inflate(R.menu.note_context_menu, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {
                    showDeleteConfirmationDialog(note)
                    true
                }
                R.id.menu_move -> {
                    showMoveNoteDialog(note)
                    true
                }
                R.id.menu_share -> {
                    shareNote(note)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun showDeleteConfirmationDialog(note: Note) {
        AlertDialog.Builder(requireContext())
            .setTitle("Удаление заметки")
            .setMessage("Вы уверены, что хотите удалить заметку \"${note.title}\"?")
            .setPositiveButton("Удалить") { _, _ ->
                viewModel.deleteNote(note)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun showMoveNoteDialog(note: Note) {
        // TODO: Реализовать диалог выбора целевой записной книжки
        Toast.makeText(requireContext(), "Функция перемещения в разработке", Toast.LENGTH_SHORT).show()
    }

    private fun shareNote(note: Note) {
        // TODO: Реализовать функциональность "поделиться"
        Toast.makeText(requireContext(), "Функция поделиться в разработке", Toast.LENGTH_SHORT).show()
    }

    private fun toggleEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.emptyList.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
        } else {
            binding.emptyList.visibility = View.GONE
            binding.recyclerView.visibility = View.VISIBLE
        }
    }
}