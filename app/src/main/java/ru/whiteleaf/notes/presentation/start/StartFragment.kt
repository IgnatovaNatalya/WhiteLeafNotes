package ru.whiteleaf.notes.presentation.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.interfaces.ContextNotebookActionHandler
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.databinding.FragmentStartBinding
import ru.whiteleaf.notes.domain.model.Note
import ru.whiteleaf.notes.domain.model.Notebook
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.whiteleaf.notes.data.model.RecentNote

class StartFragment : BindingFragment<FragmentStartBinding>(), ContextNoteActionHandler,
    ContextNotebookActionHandler {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStartBinding {
        return FragmentStartBinding.inflate(inflater, container, false)
    }

    private val viewModel: StartViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
    }

    private fun setupRecyclerView() {
        val adapter = StartAdapter(
            onRecentNoteClicked = { recentNote -> navigateToRecentNote(recentNote) },
            onShowMoreRecent = { viewModel.showMoreRecent() },

            onAllNotebooksClicked = { findNavController().navigate(StartFragmentDirections.actionGlobalNotebooksFragment()) },
            onNotebookClicked = { notebook -> navigateToNotebook(notebook) },
            onShowMoreNotebooks = { viewModel.showMoreNotebooks() },

            onRootNotesClicked = { navigateToRootNotes() },
            onNoteClicked = { note -> navigateToNote(note) },
            onShowMoreNotes = { viewModel.showMoreNotes() },

            contextNoteActionHandler = this,
            contextNotebookActionHandler = this,
        )

        binding.startRecyclerView.adapter = adapter
        binding.startRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.startScreenState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            renderEvent(event)
        }

        viewModel.message.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun renderEvent(event: StartNavigationEvent) {
        when (event) {
            StartNavigationEvent.Idle -> {}

            is StartNavigationEvent.NavigateToCreatedNotebook -> {}

            is StartNavigationEvent.NavigateToCreatedNote -> {
                event.note.let {
                    val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
                        noteId = it.id,
                        notebookPath = null
                    )
                    findNavController().navigate(action)
                    viewModel.onNavigated()
                }

            }

            is StartNavigationEvent.ShareUri -> {
                event.uri.let {
                    ShareHelper.shareFile(requireContext(), it, "Поделиться архивом ZIP")
                    viewModel.onNavigated()
                }
            }
        }
    }

    private fun renderState(state: StartScreenState) {
        when (state) {
            StartScreenState.Loading -> {
                binding.startProgressBar.visibility = View.VISIBLE
                binding.startRecyclerView.visibility = View.GONE
            }

            is StartScreenState.Success -> {
                binding.startProgressBar.visibility = View.GONE
                binding.startRecyclerView.visibility = View.VISIBLE
                (binding.startRecyclerView.adapter as StartAdapter).submitList(state.startScreenItems)
            }
        }
    }

    private fun navigateToRecentNote(note: RecentNote) {
        val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
            noteId = note.id,
            notebookPath = note.notebookPath
        )
        findNavController().navigate(action)
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = StartFragmentDirections.actionStartFragmentToNoteListFragment(notebook.path)
        findNavController().navigate(action)
    }

    private fun navigateToRootNotes() {
        val action = StartFragmentDirections.actionStartFragmentToNoteListFragment("")
        findNavController().navigate(action)
    }

    private fun navigateToNote(note: Note) {
        val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
            noteId = note.id,
            notebookPath = null
        )
        findNavController().navigate(action)
    }

    private fun createNewNote() { //todo может сделать FAB
        viewModel.createNewNote()
    }

    override fun onRenameNote(note: Note) {
        DialogHelper.createRenameNoteDialog(requireContext(), note.title) { newTitle ->
            viewModel.updateNoteTitle(note, newTitle)
        }
            .show()
    }

    override fun onDeleteNotebook(notebook: Notebook) {
        val dialog = DialogHelper.createDeleteNotebookDialog(
            context = requireContext(),
            notebookTitle = notebook.path,
            onDeleteConfirmed = { viewModel.deleteNotebook(notebook, requireContext()) }
        )
        dialog.show()
    }

    override fun onRenameNotebook(notebook: Notebook) {
        DialogHelper.createRenameNotebookDialog(requireContext(), notebook.path) { newName ->
            viewModel.renameNotebook(notebook, newName, requireContext())
        }.show()
    }

    override fun onMoveNote(note: Note) {
        val dialog = DialogHelper.createMoveNoteDialog(
            requireContext(),
            viewModel.getAllNotebooks(),
            note.notebookPath?:""
        ) { newNotebookName ->
            viewModel.moveNote(note, newNotebookName)
        }
        dialog.show()
    }

    override fun onDeleteNote(note: Note) {
        val dialog = DialogHelper.createDeleteNoteConfirmationDialog(
            context = requireContext(),
            noteTitle = note.title,
            onDeleteConfirmed = { viewModel.deleteNote(note) }
        )
        dialog.show()
    }

    override fun onShareNote(note: Note) {
        if (note.isNotEmpty()) ShareHelper.shareNote(requireContext(), note)
        else Toast.makeText(requireContext(), "Пустая заметка", Toast.LENGTH_SHORT).show()
    }

    override fun onShareNotebook(notebook: Notebook) = viewModel.shareNotebook(notebook.path)

    override fun onResume() {
        super.onResume()

        viewModel.loadData()
    }
}
