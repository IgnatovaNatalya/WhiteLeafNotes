package ru.whiteleaf.notes.presentation.start

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
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
import ru.whiteleaf.notes.common.utils.DialogHelper.createCreateNotebookDialog
import ru.whiteleaf.notes.common.utils.DialogHelper.createChangeDateDialog
import ru.whiteleaf.notes.common.utils.toggleSecurePreview
import ru.whiteleaf.notes.data.model.RecentNote
import ru.whiteleaf.notes.domain.model.NoteFound
import ru.whiteleaf.notes.presentation.root.RootActivity
import ru.whiteleaf.notes.presentation.search.NoteSearchAdapter
import ru.whiteleaf.notes.presentation.search.SearchableFragment

class StartFragment : BindingFragment<FragmentStartBinding>(), ContextNoteActionHandler,
    ContextNotebookActionHandler, SearchableFragment {

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentStartBinding {
        return FragmentStartBinding.inflate(inflater, container, false)
    }

    private val viewModel: StartViewModel by viewModel()
    private lateinit var noteSearchAdapter: NoteSearchAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        toggleSecurePreview(requireActivity(), false) //на стартовом экране всегда не зашифровано

        setupFab()
        setupStartRecyclerView()
        setupSearchRecyclerView()
        setupObservers()
    }

    private fun setupFab() {
        binding.startCreateNote.setOnClickListener { viewModel.createNewNote() }
    }

    private fun setupStartRecyclerView() {
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
            onCreateNotebookClicked = {
                createCreateNotebookDialog(requireContext()) { name ->
                    viewModel.createNotebook(name)
                }.show()
            },
        )

        binding.startRecyclerView.adapter = adapter
        binding.startRecyclerView.layoutManager = LinearLayoutManager(requireContext())
    }

    private fun setupSearchRecyclerView() {
        noteSearchAdapter = NoteSearchAdapter(
            onFoundNoteClicked = { noteFound ->
                navigateToNoteFound(noteFound)
            },
            onNoteClicked = { note -> navigateToNote(note) },

            onFoundNotebookClicked = { notebook -> navigateToNotebook(notebook) },

            modeGlobal = true
        )

        binding.searchRecyclerView.adapter = noteSearchAdapter
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        binding.searchRecyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }


    private fun setupObservers() {
        viewModel.startScreenState.observe(viewLifecycleOwner) { state ->
            renderState(state)
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event ->
            renderEvent(event)
        }
    }

    override fun onSearchQueryChanged(query: String) {
        println("DEBUG: Start Fragment: onSearchQueryChanged: query=$query")
        if (query.isEmpty()) viewModel.prepareSearch()
        if (query.length >= 3) viewModel.onSearchQueryChanged(query)
    }

    override fun onSearchQuerySubmitted(query: String) {
        println("DEBUG: Start Fragment: onSearchQuerySubmitted: query=$query")
        if (query.length >= 3) viewModel.onSearchQuerySubmitted(query)
    }

    override fun onSearchCleared() = viewModel.loadData()

    override fun onSearchStarted() = viewModel.prepareSearch()


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
                    viewModel.clearEvent()
                }

            }

            is StartNavigationEvent.ShareUri -> {
                event.uri.let {
                    ShareHelper.shareFile(requireContext(), it, "Поделиться архивом ZIP")
                    viewModel.clearEvent()
                }
            }

            is StartNavigationEvent.ShowMessage -> {
                Toast.makeText(requireContext(), event.msg, Toast.LENGTH_SHORT).show()
                viewModel.clearEvent()
            }
        }
    }

    private fun renderState(state: StartScreenState) {
        when (state) {
            StartScreenState.Loading -> {
                binding.startProgressBar.visibility = View.VISIBLE
                binding.startRecyclerView.visibility = View.GONE
                binding.searchRecyclerView.visibility = View.GONE
                binding.searchHeaderStart.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.startCreateNote.visibility = View.GONE
            }

            is StartScreenState.Success -> {
                binding.startProgressBar.visibility = View.GONE
                binding.startRecyclerView.visibility = View.VISIBLE
                (binding.startRecyclerView.adapter as StartAdapter).submitList(state.startScreenItems)
                binding.searchRecyclerView.visibility = View.GONE
                binding.searchHeaderStart.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.startCreateNote.visibility = View.VISIBLE
            }

            is StartScreenState.SearchResults -> {
                binding.startProgressBar.visibility = View.GONE
                binding.startRecyclerView.visibility = View.GONE
                binding.searchHeaderStart.visibility = View.VISIBLE
                if (state.foundItems.isNotEmpty()) {
                    binding.searchRecyclerView.visibility = View.VISIBLE
                    noteSearchAdapter.submitList(state.foundItems)
                    binding.emptyList.visibility = View.GONE
                } else {
                    binding.searchRecyclerView.visibility = View.GONE
                    binding.emptyList.visibility = View.VISIBLE
                }
                (requireActivity() as RootActivity).toggleSearchView(true, state.query)
                binding.startCreateNote.visibility = View.GONE
            }

            StartScreenState.SearchIdle -> {
                binding.startProgressBar.visibility = View.GONE
                binding.startRecyclerView.visibility = View.GONE
                binding.searchRecyclerView.visibility = View.GONE
                binding.searchHeaderStart.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE
                binding.startCreateNote.visibility = View.GONE

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
            notebookPath = note.notebookPath
        )
        findNavController().navigate(action)
    }

    fun navigateToNoteFound(noteFound: NoteFound) {
        val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
            noteId = noteFound.id,
            notebookPath = noteFound.notebookPath,
            contentPosition = noteFound.contentPosition ?: 0,
            searchQuery = noteFound.query
        )
        findNavController().navigate(action)
    }

    override fun onRenameNote(note: Note) {
        DialogHelper.createRenameNoteDialog(requireContext(), note.title) { newTitle ->
            viewModel.updateNoteTitle(note, newTitle)
        }.show()
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

    override fun onMoveNote(note: Note) {
        val dialog = DialogHelper.createMoveNoteDialog(
            requireContext(),
            viewModel.getAllNotebooks(),
            note.notebookPath ?: ""
        ) { targetNotebookPath ->
            viewModel.moveNote(requireContext(), note, targetNotebookPath)
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

    override fun onChangeNoteDate(note: Note) {
        println("DEBUG: StartFragment: option change date clicked")
        val dialog = createChangeDateDialog(
            note.modifiedAt,
            { date -> viewModel.updateNoteDate(note, date) }
        )
        dialog.show(childFragmentManager, "date_picker")
    }

    override fun onShareNote(note: Note) {
        if (note.isNotEmpty()) ShareHelper.shareNote(requireContext(), note)
        else Toast.makeText(requireContext(), "Пустая заметка", Toast.LENGTH_SHORT).show()
    }

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
        viewModel.resumeScreen()
    }
}
