package com.example.txtnotesapp.presentation.note_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.FragmentNoteListBinding
import com.example.txtnotesapp.domain.model.Note
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class NoteListFragment : Fragment() {
    private lateinit var binding: FragmentNoteListBinding

//    private val viewModel: NoteListViewModel by viewModel(parameters = {
//        val notebookPath = arguments?.getString("notebook_path")
//        notebookPath
//    })

    private val viewModel: NoteListViewModel by viewModel{
        parametersOf(requireArguments().getString("notebook_path"))
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentNoteListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //val adapter = NoteAdapter { note -> openNote(note) }
        val adapter = NoteAdapter(
            onNoteClicked = { note ->
                // Открыть заметку для редактирования
                openNote(note)
            },
            onNoteLongClicked = { note ->
                // Показать контекстное меню
                showContextMenu(note)
            }
        )
        binding.recyclerView.adapter = adapter

        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            if (notes.isEmpty()) {
                binding.emptyList.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyList.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                adapter.submitList(notes)
            }
        }

        binding.createNote.setOnClickListener { createNewNote() }
    }


    private fun createNewNote() {
        // Создание новой заметки
    }

    private fun openNote(note: Note) {
        // Навигация к экрану редактирования
        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditFragment(note.title)
        findNavController().navigate(action)
    }

    private fun showContextMenu(note: Note) {
        val menu = PopupMenu(requireContext(), binding.root)
        menu.inflate(R.menu.note_context_menu)
        menu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_delete -> {
                    //viewModel.deleteNote(note) todo
                    true
                }
                R.id.menu_edit -> {
                    // Реализация редактирования todo
                    true
                }
                R.id.menu_move -> {
                    // Реализация перемещения todo
                    true
                }
                R.id.menu_share -> {
                    // Реализация поделиться todo
                    true
                }
                else -> false
            }
        }
        menu.show()
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