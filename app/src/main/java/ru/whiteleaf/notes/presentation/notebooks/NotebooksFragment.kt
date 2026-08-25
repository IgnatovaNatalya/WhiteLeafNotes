package ru.whiteleaf.notes.presentation.notebooks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import org.koin.androidx.viewmodel.ext.android.viewModel
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.interfaces.ContextNotebookActionHandler
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.DialogHelper.createCreateNotebookDialog
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.databinding.FragmentNotebooksBinding
import ru.whiteleaf.notes.domain.model.Notebook

class NotebooksFragment : BindingFragment<FragmentNotebooksBinding>(),
    ContextNotebookActionHandler {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNotebooksBinding {
        return FragmentNotebooksBinding.inflate(inflater, container, false)
    }

    private val viewModel: NotebooksViewModel by viewModel()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupFab()
        setupRecyclerView()
        setupObservers()
    }

    private fun setupFab() {
        binding.notebooksCreateNotebook.setOnClickListener {
            createCreateNotebookDialog(requireContext()) { name ->
                viewModel.createNotebook(name)
            }.show()
        }
    }

    private fun setupRecyclerView() {
        val adapter = NotebooksAdapter(
            onNotebookClicked = { notebook -> navigateToNotebook(notebook) },
            contextActionHandler = this
        )

        binding.notebooksRecyclerView.adapter = adapter
        binding.notebooksRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupObservers() {
        viewModel.notebooksScreenState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            renderEvent(event)
        }
    }


    private fun renderState(state: NotebooksScreenState) {
        when (state) {
            NotebooksScreenState.Loading -> {
                binding.notebooksProgressBar.visibility = View.VISIBLE
                binding.notebooksRecyclerView.visibility = View.GONE
                binding.emptyNotebooks.visibility = View.GONE
            }

            is NotebooksScreenState.Success -> {
                binding.notebooksProgressBar.visibility = View.GONE
                binding.notebooksRecyclerView.visibility = View.VISIBLE
                (binding.notebooksRecyclerView.adapter as NotebooksAdapter).submitList(state.notebooks)
                binding.emptyNotebooks.visibility =
                    if (state.notebooks.size > 0) View.GONE else View.VISIBLE
            }

            is NotebooksScreenState.Error -> {
                binding.notebooksProgressBar.visibility = View.GONE
                binding.notebooksRecyclerView.visibility = View.GONE
                binding.emptyNotebooks.visibility = View.VISIBLE
                binding.emptyNotebooks.text = state.message
            }
        }
    }

    private fun renderEvent(event: NotebooksNavigationEvent) {
        when (event) {
            NotebooksNavigationEvent.Idle -> {}

            is NotebooksNavigationEvent.NavigateToCreatedNotebook -> {
                navigateToNotebook(event.notebook)
                viewModel.clearEvent()
            }

            is NotebooksNavigationEvent.ShareUri -> {
                event.uri.let {
                    ShareHelper.shareFile(requireContext(), it, "Поделиться архивом ZIP")
                    viewModel.clearEvent()
                }
            }

            is NotebooksNavigationEvent.ShowMessage -> {
                event.msg.let {
                    Toast.makeText(requireContext(), event.msg, Toast.LENGTH_SHORT).show()
                    viewModel.clearEvent()
                }
            }
        }
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = NotebooksFragmentDirections.actionNotebooksFragmentToNoteListFragment(notebook.path)
        findNavController().navigate(action)
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

    override fun onPinNotebook(notebook: Notebook) {
        viewModel.pinNotebook(notebook)
    }

    override fun onUnpinNotebook(notebook: Notebook) {
        viewModel.unpinNotebook(notebook)
    }

    override fun onEncryptNotebook(notebook: Notebook) =
        viewModel.encryptNotebook(requireContext(), notebook)

    override fun onDecryptNotebook(notebook: Notebook) =
        viewModel.decryptNotebook(requireContext(), notebook)

    override fun onExportNotebook(notebook: Notebook) {
        DialogHelper.createExportDialog(
            requireContext(),
            viewModel.exportPath,
            notebook.path,
            0,
        ) { shareFile, _, password ->
            viewModel.exportNotebook(
                requireContext(),
                notebook.path,
                shareFile,
                password
            )
        }.show()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

}