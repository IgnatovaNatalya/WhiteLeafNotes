package com.example.txtnotesapp.presentation.start

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.txtnotesapp.R
import com.example.txtnotesapp.common.classes.BindingFragment
import com.example.txtnotesapp.common.interfaces.ContextActionHandler
import com.example.txtnotesapp.common.utils.DialogHelper
import com.example.txtnotesapp.common.utils.DialogHelper.ShareHelper
import com.example.txtnotesapp.databinding.FragmentStartBinding
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook
import org.koin.androidx.viewmodel.ext.android.viewModel

class StartFragment : BindingFragment<FragmentStartBinding>(), ContextActionHandler {

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

        binding.buttonSettings.setOnClickListener {
            val action = StartFragmentDirections.actionStartFragmentToSettingsFragment()
            findNavController().navigate(action)
        }
    }

    private fun setupRecyclerView() {
        val adapter = StartAdapter(
            onNotebookClicked = { notebook -> navigateToNotebook(notebook) },
            onNoteClicked = { note -> navigateToNote(note) },
            onAddNotebookClicked = { showCreateNotebookDialog() },
            onAddNoteClicked = { createNewNote() },
            contextActionHandler = this
        )

        binding.startRecyclerView.adapter = adapter
        binding.startRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.startItems.observe(viewLifecycleOwner) { items ->
            (binding.startRecyclerView.adapter as StartAdapter).submitList(items)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.startProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        }

        viewModel.navigateToCreatedNote.observe(viewLifecycleOwner) { note ->
            note?.let {
                //val action = StartFragmentDirections.actionGlobalNoteEditFragment(
                val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
                    noteId = note.id,
                    notebookPath = null
                )
                findNavController().navigate(action)
                viewModel.onNoteNavigated()
            }
        }

        viewModel.navigateToCreatedNotebook.observe(viewLifecycleOwner) { notebook ->
            notebook?.let {
                //val action = StartFragmentDirections.actionGlobalNoteListFragment(
                val action = StartFragmentDirections.actionStartFragmentToNoteListFragment(notebook.path)
                findNavController().navigate(action)
                viewModel.onNotebookNavigated()
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = StartFragmentDirections.actionStartFragmentToNoteListFragment(notebook.path)
        findNavController().navigate(action)
    }

    private fun navigateToNote(note: Note) {
        val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
            noteId = note.id,
            notebookPath = null
        )
        findNavController().navigate(action)
    }

    private fun showCreateNotebookDialog() {
        val dialogView =
            LayoutInflater.from(requireContext()).inflate(R.layout.dialog_create_notebook, null)
        val editText = dialogView.findViewById<EditText>(R.id.notebook_name)

        AlertDialog.Builder(requireContext())
            .setTitle("Создать записную книжку")
            .setView(dialogView)
            .setPositiveButton("Создать") { dialog, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    viewModel.createNewNotebook(name)
                } else {
                    Toast.makeText(requireContext(), "Введите название книжки", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createNewNote() {
        viewModel.createNewNote()
    }

    override fun onRenameNote(note: Note) {
        DialogHelper.createRenameNoteDialog(requireContext(), note.title) { newTitle ->
            viewModel.updateNoteTitle(note, newTitle)
        }
            .show()
    }

    override fun onDeleteNotebook(notebook: Notebook) {
        val dialog = DialogHelper.createDeleteNotebookConfirmationDialog(
            context = requireContext(),
            notebookTitle = notebook.name,
            onDeleteConfirmed = { viewModel.deleteNotebook(notebook) }
        )
        dialog.show()
    }

    override fun onRenameNotebook(notebook: Notebook) {
        DialogHelper.createRenameNotebookDialog(requireContext(), notebook.name) { newName ->
            viewModel.renameNotebook(notebook, newName)
        }
            .show()
    }

    override fun onMoveNote(note: Note) {
        val dialog = DialogHelper.createMoveNoteDialog(requireContext()) { newNotebookName ->
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

    override fun onShareNote(note: Note) = ShareHelper.shareNote(requireContext(), note)

    override fun onResume() {
        super.onResume()
        viewModel.reloadNotes()
    }
}
