package com.example.whiteleafnotes.presentation.note_list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.whiteleafnotes.R
import com.example.whiteleafnotes.common.classes.BindingFragment
import com.example.whiteleafnotes.common.interfaces.ContextNoteActionHandler
import com.example.whiteleafnotes.common.utils.ContextMenuHelper
import com.example.whiteleafnotes.common.utils.DialogHelper
import com.example.whiteleafnotes.common.utils.ShareHelper
import com.example.whiteleafnotes.databinding.FragmentNoteListBinding
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.presentation.settings.ExportState
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf


class NoteListFragment : BindingFragment<FragmentNoteListBinding>(), ContextNoteActionHandler {

    private val viewModel: NoteListViewModel by viewModel { parametersOf(args.notebookPath) }
    private val args: NoteListFragmentArgs by navArgs()
    private var notebookTitle = ""

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNoteListBinding {
        return FragmentNoteListBinding.inflate(inflater, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) { ///?
        super.onCreate(savedInstanceState)
        //setHasOptionsMenu(true) // Важно: разрешаем фрагменту работать с меню
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.notebookPath
        notebookTitle = args.notebookPath.toString()

        setupOptionsMenu()
        setupObservers()
        setupRecyclerView()
        setupFab()
    }

    private fun setupObservers() {

        viewModel.notes.observe(viewLifecycleOwner) { notes ->
            toggleEmptyState(notes.isEmpty())
            (binding.recyclerView.adapter as NoteAdapter).submitList(notes)
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

        viewModel.notebookRenamed.observe(viewLifecycleOwner) { path ->
            val action = NoteListFragmentDirections.actionGlobalNoteListFragment(path)
            findNavController().navigate(action)
        }

        viewModel.notebookDeleted.observe(viewLifecycleOwner) {
            findNavController().navigateUp()
        }

        viewModel.shareNotebookState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ExportState.Idle -> {
                    hideProgress()
                }

                is ExportState.Loading -> {
                    showProgress()
                    Toast.makeText(requireContext(), "Создание архива...", Toast.LENGTH_SHORT)
                        .show()
                }

                is ExportState.Success -> {
                    hideProgress()
                    Toast.makeText(requireContext(), "Архив создан успешно", Toast.LENGTH_SHORT)
                        .show()
                    shareExportFile(state.fileUri)
                }

                is ExportState.Error -> {
                    hideProgress()
                    Toast.makeText(requireContext(), "Ошибка экспорта", Toast.LENGTH_SHORT).show()
                }
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
    }

    private fun setupRecyclerView() {
        val adapter = NoteAdapter(
            onNoteClicked = { note -> viewModel.onNoteClicked(note.id) },
            contextActionHandler = this
        )

        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupOptionsMenu() {
        val optionsButton = requireActivity().findViewById<ImageButton>(R.id.btn_options_menu)
        optionsButton?.setOnClickListener {// showOptionsMenu(optionsButton)
            ContextMenuHelper.showPopupMenu(
                context = requireContext(),
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuItemsNoteList(optionsButton.context),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_create_note -> onOptionsCreateNote()
                        R.id.options_rename_notebook -> onOptionsRenameNotebook()
                        R.id.options_share_notebook -> onOptionsShareNotebook()
                        R.id.options_delete_notebook -> onOptionsDeleteNotebook()
                    }
                }
            )
        }
    }

    private fun setupFab() {
        binding.createNote.setOnClickListener {
            viewModel.createNewNote()
        }
    }

    private fun onOptionsCreateNote() = viewModel.createNewNote()

    private fun onOptionsRenameNotebook() {
        DialogHelper.createRenameNotebookDialog(requireContext(), notebookTitle)
        { newName -> viewModel.renameNotebook(newName) }.show()
    }

    private fun onOptionsShareNotebook() = viewModel.shareNotebook()

    private fun onOptionsDeleteNotebook() {
        DialogHelper.createDeleteNotebookDialog(
            context = requireContext(),
            notebookTitle = notebookTitle,
            onDeleteConfirmed = { viewModel.deleteNotebook() }
        ).show()
    }

    override fun onRenameNote(note: Note) {
        DialogHelper.createRenameNoteDialog(
            requireContext(),
            note.title
        ) { newTitle -> viewModel.updateNoteTitle(note, newTitle) }.show()
    }

    override fun onMoveNote(note: Note) {
        DialogHelper.createMoveNoteDialog(requireContext()) { newNotebookName ->
            viewModel.moveNote(note, newNotebookName)
        }.show()
    }

    override fun onDeleteNote(note: Note) {
        DialogHelper.createDeleteNoteConfirmationDialog(
            context = requireContext(),
            noteTitle = note.title,
            onDeleteConfirmed = { viewModel.deleteNote(note) }
        ).show()
    }

    override fun onShareNote(note: Note) = ShareHelper.shareNote(requireContext(), note)

    private fun shareExportFile(uri: Uri?) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/zip"
        }
        startActivity(Intent.createChooser(shareIntent, "Поделиться архивом"))
    }

    private fun navigateToNoteEdit(noteId: String) {
        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditFragment(
            noteId = noteId,
            notebookPath = args.notebookPath
        )
        findNavController().navigate(action)
    }

    private fun navigateToNoteCreated(noteId: String) {
        val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
            noteId = noteId,
            notebookPath = args.notebookPath
        )
        findNavController().navigate(action)
    }

    private fun showProgress() {
        binding.noteListProgressBar.visibility = View.VISIBLE
    }

    private fun hideProgress() {
        binding.noteListProgressBar.visibility = View.GONE
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

    override fun onResume() {
        super.onResume()
        viewModel.reloadNotes()
    }
}