package ru.whiteleaf.notes.presentation.note_list

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.interfaces.ContextNoteActionHandler
import ru.whiteleaf.notes.common.utils.ContextMenuHelper
import ru.whiteleaf.notes.common.utils.DialogHelper
import ru.whiteleaf.notes.common.utils.ShareHelper
import ru.whiteleaf.notes.databinding.FragmentNoteListBinding
import ru.whiteleaf.notes.domain.model.Note
import org.koin.androidx.viewmodel.ext.android.viewModel
import org.koin.core.parameter.parametersOf
import ru.whiteleaf.notes.common.utils.DialogHelper.createChangeDateDialog
import ru.whiteleaf.notes.common.utils.toggleSecurePreview
import ru.whiteleaf.notes.presentation.note_list.grid.NotesGridAdapter
import ru.whiteleaf.notes.presentation.note_list.linear.NotesLinearAdapter
import ru.whiteleaf.notes.presentation.search.NoteSearchAdapter


class NoteListFragment : BindingFragment<FragmentNoteListBinding>(), ContextNoteActionHandler {

    private val viewModel: NoteListViewModel by viewModel { parametersOf(args.notebookPath) }
    private val args: NoteListFragmentArgs by navArgs()
    private var notebookPath = ""
    //private var isEncrypted = false

    private lateinit var noteLinearAdapter: NotesLinearAdapter
    private lateinit var plannerAdapter: NotesGridAdapter
    private lateinit var noteSearchAdapter: NoteSearchAdapter

    private lateinit var btnLockIndicator: ImageButton

    private var navigateToNote = false
    private var isPlannerView = false

    override fun createBinding(
        inflater: LayoutInflater,
        container: ViewGroup?
    ): FragmentNoteListBinding {
        return FragmentNoteListBinding.inflate(inflater, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navigateToNote = false
        notebookPath = args.notebookPath.toString()

        setupTitleAndViewMode()
        setupObservers()
        setupOptionsMenu()
        setupListRecyclerView()
        setupPlannerRecyclerView()
        setupSearchRecyclerView()
        setupClickListeners()
    }

    private fun setupTitleAndViewMode() {
        val actionBar = (requireActivity() as AppCompatActivity).supportActionBar

        actionBar?.title = notebookPath
        if (notebookPath != "") actionBar?.subtitle = "Записная книжка"


        btnLockIndicator =
            (requireActivity() as AppCompatActivity).findViewById(R.id.btn_lock_indicator)

        val isEncrypted = viewModel.getEncryptionStatus()

        btnLockIndicator.visibility = if (isEncrypted) View.VISIBLE else View.GONE

        isPlannerView = viewModel.getViewMode()
        println("DEBUG: Fragment onViewCreated viewMode is planner = $isPlannerView")

        toggleSecurePreview(requireActivity(), isEncrypted)
    }

    private fun setupObservers() {

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }
        viewModel.navigationEvent.observe(viewLifecycleOwner) { event -> renderEvent(event) }

        viewModel.noteListState.observe(viewLifecycleOwner) { state -> renderState(state) }
    }

    private fun setupListRecyclerView() {
        noteLinearAdapter = NotesLinearAdapter(
            onNoteClicked = { note -> viewModel.onNoteClicked(note.id) },
            contextActionHandler = this
        )

        binding.recyclerViewList.adapter = noteLinearAdapter
        binding.recyclerViewList.layoutManager = LinearLayoutManager(requireContext())

        binding.recyclerViewList.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupPlannerRecyclerView() {

        plannerAdapter = NotesGridAdapter(
            onNoteClickListener = { note -> viewModel.onNoteClicked(note.id) },
            contextActionHandler = this,
        )

        val spanCount = 4
        val layoutManager = GridLayoutManager(requireContext(), spanCount)

        // Управляем шириной header'ов (они должны занимать всю ширину)
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (plannerAdapter.getItemViewType(position)) {
                    plannerAdapter.TYPE_HEADER -> spanCount // header на всю ширину
                    else -> 1 // заметка занимает 1 колонку
                }
            }
        }

        binding.recyclerViewPlanner.apply {
            this.layoutManager = layoutManager
            adapter = plannerAdapter
        }
    }

    private fun setupSearchRecyclerView() {
        noteSearchAdapter = NoteSearchAdapter(
            onFoundNoteClicked = { noteFound ->
                viewModel.onNoteFoundClicked(noteFound.id, noteFound.contentPosition ?: 0)
            },
            onNoteClicked = { note -> viewModel.onNoteClicked(note.id) },
            onFoundNotebookClicked = {},
        )

        binding.recyclerViewSearch.adapter = noteSearchAdapter
        binding.recyclerViewSearch.layoutManager = LinearLayoutManager(requireContext())

        binding.recyclerViewSearch.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    private fun setupOptionsMenu() {
        val optionsButton = requireActivity().findViewById<ImageButton>(R.id.btn_options_menu)
        optionsButton?.setOnClickListener {
            ContextMenuHelper.showPopupMenu(
                context = requireContext(),
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuItemsNoteList(
                    optionsButton.context,
                    isProtected = viewModel.getEncryptionStatus(),
                    isPlannerView = isPlannerView
                ),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_create_note -> onOptionsCreateNote()
                        R.id.options_view_list -> switchViewMode(false)
                        R.id.options_view_planner -> switchViewMode(true)
                        R.id.options_rename_notebook -> onOptionsRenameNotebook()
                        R.id.options_unprotect_notebook -> onOptionsDecryptNotebook()
                        R.id.options_protect_notebook -> onOptionsEncryptNotebook()
                        R.id.options_export_notebook -> onOptionsShareNotebook()
                        R.id.options_delete_notebook -> onOptionsDeleteNotebook()
                    }
                }
            )
        }
    }

    private fun switchViewMode(mode: Boolean) {
        isPlannerView = mode
        viewModel.setViewMode(mode)
    }

    private fun setupClickListeners() {
        binding.createNote.setOnClickListener {
            viewModel.createNewNote()
        }

        btnLockIndicator.setOnClickListener {
            viewModel.lockNotebook()
        }

        binding.unlockButton.setOnClickListener {
            viewModel.unlockNotebook(requireActivity(), UnlockTarget.ToLoad)
        }
    }

    private fun onOptionsCreateNote() {
        navigateToNote = true
        viewModel.createNewNote()
    }

    private fun onOptionsRenameNotebook() {
        DialogHelper.createRenameNotebookDialog(requireContext(), notebookPath)
        { newName -> viewModel.renameNotebook(newName, requireContext()) }.show()
    }

    private fun onOptionsEncryptNotebook() = viewModel.encryptNotebook(requireContext())
    private fun onOptionsDecryptNotebook() = viewModel.decryptNotebook(requireContext())

    private fun onOptionsShareNotebook() {
        DialogHelper.createExportDialog(
            requireContext(),
            viewModel.exportPath,
            notebookPath,
            0,
        ) { shareFile, _, password ->
            viewModel.exportNotebook(
                requireContext(),
                shareFile,
                password
            )
        }.show()
    }

    private fun onOptionsDeleteNotebook() {
        DialogHelper.createDeleteNotebookDialog(
            context = requireContext(),
            notebookTitle = notebookPath,
            onDeleteConfirmed = { viewModel.deleteNotebook(requireContext()) }
        ).show()
    }

    override fun onRenameNote(note: Note) {
        DialogHelper.createRenameNoteDialog(
            requireContext(),
            note.title
        ) { newTitle -> viewModel.updateNoteTitle(note, newTitle) }.show()
    }

    override fun onChangeNoteDate(note: Note) {
        createChangeDateDialog(
            note.modifiedAt,
            { date -> viewModel.updateNoteDate(note, date) }
        ).show(childFragmentManager, "date_picker")
    }

    override fun onMoveNote(note: Note) {
        DialogHelper.createMoveNoteDialog(
            requireContext(),
            viewModel.getAllNotebooks(),
            note.notebookPath ?: ""
        ) { targetNotebookPath ->
            viewModel.moveNote(requireContext(), note, targetNotebookPath)
        }.show()
    }

    override fun onDeleteNote(note: Note) {
        DialogHelper.createDeleteNoteConfirmationDialog(
            context = requireContext(),
            noteTitle = note.title,
            onDeleteConfirmed = { viewModel.deleteNote(note) }
        ).show()
    }

    override fun onShareNote(note: Note) {
        if (note.isNotEmpty()) ShareHelper.shareNote(requireContext(), note)
        else Toast.makeText(requireContext(), "Пустая заметка", Toast.LENGTH_SHORT).show()
    }

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

    private fun navigateToNoteFound(noteId: String, contentPosition: Int) {
        val action = NoteListFragmentDirections.actionNoteListFragmentToNoteEditFragment(
            noteId = noteId,
            notebookPath = args.notebookPath,
            contentPosition = contentPosition
        )
        findNavController().navigate(action)
    }

    private fun reopenNotebook(newPath: String) {
        val action = NoteListFragmentDirections.actionGlobalNoteListFragment(newPath)
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.noteListFragment, inclusive = true) // удаляем текущий NoteListFragment
            .build()
        findNavController().navigate(action, navOptions)
    }

    private fun renderState(state: NoteListState) {
        println("👀 Fragment observed state: ${state.javaClass.simpleName}")
        when (state) {
            is NoteListState.Success -> {

                if (isPlannerView) {
                    binding.recyclerViewList.visibility = View.GONE
                    binding.recyclerViewPlanner.visibility = View.VISIBLE
                    plannerAdapter.submitList(state.notes)
                } else {
                    binding.recyclerViewList.visibility = View.VISIBLE
                    binding.recyclerViewPlanner.visibility = View.GONE
                    noteLinearAdapter.submitList(state.notes)
                }
                binding.recyclerViewSearch.visibility = View.GONE

                println("✅ Fragment showing ${state.notes.size} notes")
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility =
                    if (state.notes.isNotEmpty()) View.GONE else View.VISIBLE
                binding.notebookProtected.visibility = View.GONE

                if (btnLockIndicator.isVisible)
                    btnLockIndicator.setImageResource(R.drawable.ic_ind_unlocked)

                binding.createNote.visibility = View.VISIBLE
            }

            is NoteListState.Blocked -> {
                println("⏳ Fragment showing Blocked")
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.VISIBLE
                binding.createNote.visibility = View.GONE
                binding.recyclerViewList.visibility = View.GONE
                binding.recyclerViewPlanner.visibility = View.GONE
                binding.recyclerViewSearch.visibility = View.GONE
                btnLockIndicator.setImageResource(R.drawable.ic_ind_locked)
                btnLockIndicator.visibility = View.VISIBLE
            }

            is NoteListState.Error -> {
                println("❌ Fragment showing error: ${state.message}")
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.GONE
                binding.recyclerViewList.visibility = View.GONE
                binding.recyclerViewPlanner.visibility = View.GONE
                binding.recyclerViewSearch.visibility = View.GONE
//                btnLockIndicator.visibility = View.GONE
                binding.emptyList.text = state.message
            }

            NoteListState.Loading -> {
                println("⏳ Fragment showing loading")
                binding.noteListProgressBar.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.GONE
                binding.recyclerViewList.visibility = View.GONE
                binding.recyclerViewPlanner.visibility = View.GONE
                binding.recyclerViewSearch.visibility = View.GONE
//                btnLockIndicator.visibility = View.GONE
            }

            is NoteListState.SearchResults -> {
                binding.noteListProgressBar.visibility = View.GONE

                if (!state.foundNotes.isNotEmpty()) {
                    binding.emptyList.visibility = View.VISIBLE
                    binding.emptyList.text = "Не найдено"
                } else binding.emptyList.visibility = View.GONE

                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.GONE
                binding.recyclerViewList.visibility = View.GONE
                binding.recyclerViewPlanner.visibility = View.GONE

                binding.recyclerViewSearch.visibility = View.VISIBLE
                noteSearchAdapter.submitList(state.foundNotes)

//                if (isEncrypted) { !!! если во время выполнения поиска ключ заблокировался
                //                мы получим исключение и тогда нужно иконку поменять и показать биометрию
//                    btnLockIndicator.setImageResource(R.drawable.ic_ind_unlocked)
//                    btnLockIndicator.visibility = View.VISIBLE
//                } else btnLockIndicator.visibility = View.GONE

            }
        }
    }

    private fun renderEvent(event: NoteListNavigationEvent) {
        when (event) {
            is NoteListNavigationEvent.ExportLink -> shareExportFile(event.uri)

            is NoteListNavigationEvent.NavigateToNote -> {
                navigateToNote = true
                navigateToNoteEdit(event.noteId)
            }

            is NoteListNavigationEvent.NavigateToNoteFound -> {
                navigateToNote = true
                navigateToNoteFound(event.noteId, event.contentPosition)
            }

            is NoteListNavigationEvent.ReopenNotebook -> reopenNotebook(event.path)

            NoteListNavigationEvent.NavigateUp -> findNavController().navigateUp()

            is NoteListNavigationEvent.ShowBiometric ->
                viewModel.unlockNotebook(requireActivity(), event.unlockTarget)

            NoteListNavigationEvent.Idle -> {}

        }
        viewModel.onNavigated()
    }

    override fun onResume() {
        super.onResume()
        //viewModel.loadNotes()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onNotebookExited(navigateToNote)
    }
}