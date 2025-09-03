package com.example.txtnotesapp.presentation.note_list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.FragmentNoteListBinding
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.utils.PermissionUtils
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

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.notebookPath

        setupObservers()
        setupRecyclerView()
        setupFab()
    }

    private fun setupObservers() {

        viewModel.message.observe(viewLifecycleOwner) { error ->
            error?.let {
                if (it.contains("доступ")) {
                    // Показываем кнопку для запроса разрешений
                    showStoragePermissionError(it)
                } else {
                    Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                }
                viewModel.clearError()
            }
        }

        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            toggleEmptyState(notes.isEmpty())
            (binding.recyclerView.adapter as NoteAdapter).submitList(notes)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.message.observe(viewLifecycleOwner) { error ->
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

        viewModel.navigateToCreatedNote.observe(viewLifecycleOwner) { noteId ->
            noteId?.let {
                navigateToNoteCreated(noteId)
                viewModel.onNoteCreatedNavigated()
            }
        }
    }

    private fun showStoragePermissionError(message: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Ошибка доступа")
            .setMessage(message)
            .setPositiveButton("Запросить разрешение") { _, _ ->
                PermissionUtils.requestStoragePermission(requireActivity())
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.show()
    }

    private fun setupRecyclerView() {
        val adapter = NoteAdapter(
            onNoteClicked = { note ->
                viewModel.onNoteClicked(note.title)
            },
            onNoteLongClicked = { view, note ->
                showContextMenu(view, note)
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

    private fun navigateToNoteCreated(noteTitle: String) {
        val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
            noteTitle = noteTitle,
            notebookPath = args.notebookPath
        )
        findNavController().navigate(action)
    }

    private fun showContextMenu(anchorView: View, note: Note) {
        val popup = PopupMenu(requireContext(), anchorView)
        popup.menuInflater.inflate(R.menu.note_context_menu, popup.menu)

        // Настройка меню для отображения всех пунктов
        try {
            val field = popup::class.java.getDeclaredField("mPopup")
            field.isAccessible = true
            //val menuPopupHelper = field.get(popup) as MenuPopupHelper
            //menuPopupHelper.setForceShowIcon(true) // Показывать иконки если есть
        } catch (e: Exception) {
            e.printStackTrace()
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.note_menu_rename -> {
                    showRenameNoteDialog(note)
                    true
                }
                R.id.note_menu_move -> {
                    showMoveNoteDialog(note)
                    true
                }
                R.id.note_menu_share -> {
                    shareNote(note)
                    true
                }
                R.id.note_menu_delete -> {
                    showDeleteConfirmationDialog(note)
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

    private fun showRenameNoteDialog(note: Note) {
        val alertDialogBuilder = AlertDialog.Builder(requireContext())
        val renameDialogView: View = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_note_rename, null)
        alertDialogBuilder.setView(renameDialogView)
        val newTitle = renameDialogView.findViewById<EditText>(R.id.new_note_title)

        alertDialogBuilder
            .setPositiveButton("Переименовать") { _, _ ->
                viewModel.updateNoteTitle(note,newTitle.text.toString() )
            }
            .setNegativeButton("Отмена", null)
            .show()
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