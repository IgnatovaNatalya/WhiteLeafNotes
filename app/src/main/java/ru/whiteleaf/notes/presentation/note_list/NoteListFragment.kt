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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
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


class NoteListFragment : BindingFragment<FragmentNoteListBinding>(), ContextNoteActionHandler {

    private val viewModel: NoteListViewModel by viewModel { parametersOf(args.notebookPath) }
    private val args: NoteListFragmentArgs by navArgs()
    private var notebookTitle = ""
    private var isEncrypted = false

    private lateinit var noteLinearAdapter: NotesLinearAdapter
    private lateinit var plannerAdapter: NotesGridAdapter

    private lateinit var btnProtectNotebook: ImageButton
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

        (requireActivity() as AppCompatActivity).supportActionBar?.title = args.notebookPath

        notebookTitle = args.notebookPath.toString()

        btnProtectNotebook = (requireActivity() as AppCompatActivity).findViewById(R.id.btn_protect)
        btnLockIndicator =
            (requireActivity() as AppCompatActivity).findViewById(R.id.btn_lock_indicator)

        isPlannerView = viewModel.getViewMode()
        println("DEBUG: Fragment onViewCreated viewMode is planner = $isPlannerView")

        setupObservers()
        setupOptionsMenu()
        setupListRecyclerView()
        setupPlannerRecyclerView()
        setupFab()
        setupSecurityUI()
    }

    private fun setupSecurityUI() {
        btnProtectNotebook.setOnClickListener { viewModel.encryptNotebook() }
        binding.unlockButton.setOnClickListener { viewModel.unlockNotebook(requireActivity()) }
    }

    private fun setupObservers() {

        viewModel.message.observe(viewLifecycleOwner) { message ->
            message?.let {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
            }
        }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event -> navigateEvent(event) }

        viewModel.noteListState.observe(viewLifecycleOwner) { state -> renderState(state) }

        viewModel.encryptionResult.observe(viewLifecycleOwner) { result ->
            result?.onSuccess {
                Toast.makeText(requireContext(), "Записная книжка зашифрована", Toast.LENGTH_SHORT)
                    .show()
            }?.onFailure {
                Toast.makeText(
                    requireContext(),
                    "Ошибка шифрования: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun navigateEvent(event: NavigationEvent) {

        when (event) {
            NavigationEvent.Idle -> {}

            is NavigationEvent.ExportLink -> {
                shareExportFile(event.uri)
            }

            is NavigationEvent.NavigateToNote -> {
                navigateToNote = true
                navigateToNoteEdit(event.noteId)
            }

            is NavigationEvent.NavigateToNotebook -> {
                navigateToNotebook(event.path)
            }

            NavigationEvent.NavigateUp -> {
                findNavController().navigateUp()
            }
        }
        viewModel.onNavigated()
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

    private fun setupOptionsMenu() {
        val optionsButton = requireActivity().findViewById<ImageButton>(R.id.btn_options_menu)
        optionsButton?.setOnClickListener {
            ContextMenuHelper.showPopupMenu(
                context = requireContext(),
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuItemsNoteList(
                    optionsButton.context,
                    isProtected = isEncrypted,
                    isPlannerView = isPlannerView
                ),
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_create_note -> onOptionsCreateNote()
                        R.id.options_view_list -> switchViewMode(false)
                        R.id.options_view_planner -> switchViewMode(true)
                        R.id.options_rename_notebook -> onOptionsRenameNotebook()
                        R.id.options_share_notebook -> onOptionsShareNotebook()
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

    private fun setupFab() {
        binding.createNote.setOnClickListener {
            viewModel.createNewNote()
        }
    }

    private fun onOptionsCreateNote() {
        navigateToNote = true
        viewModel.createNewNote()
    }

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

    private fun navigateToNotebook(path: String) {
        val action = NoteListFragmentDirections.actionGlobalNoteListFragment(path)
        findNavController().navigate(action)
    }

    private fun showAuthenticationDialog() {
        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle("Требуется аутентификация")
            .setMessage("Для доступа к защищенному блокноту требуется отпечаток пальца")
            .setPositiveButton("Разблокировать") { _, _ ->
                viewModel.unlockNotebook(requireActivity())
            }
            .setNegativeButton("Отмена") { _, _ ->
                findNavController().navigateUp()
            }
            .setCancelable(false)
            .create()
        dialog.show()
    }

    private fun renderState(state: NoteListState) {
        println("👀 Fragment observed state: ${state.javaClass.simpleName}")
        when (state) {
            is NoteListState.Success -> {

                (requireActivity() as AppCompatActivity).supportActionBar?.subtitle =
                    if (isPlannerView) "Планирование" else "Записная книжка"

                if (isPlannerView) {
                    (requireActivity() as AppCompatActivity).supportActionBar?.subtitle =
                        "Планирование"
                    binding.recyclerViewList.visibility = View.GONE
                    binding.recyclerViewPlanner.visibility = View.VISIBLE
                    plannerAdapter.submitList(state.notes)
                } else {
                    (requireActivity() as AppCompatActivity).supportActionBar?.subtitle =
                        "Записная книжка"
                    binding.recyclerViewList.visibility = View.VISIBLE
                    binding.recyclerViewPlanner.visibility = View.GONE
                    noteLinearAdapter.submitList(state.notes)
                }

                println("✅ Fragment showing ${state.notes.size} notes")
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.GONE

                isEncrypted = state.isEncrypted
                if (state.isEncrypted) {
                    btnLockIndicator.setImageResource(R.drawable.ic_unlocked)
                    btnLockIndicator.visibility = View.VISIBLE
                } else btnLockIndicator.visibility = View.GONE

                binding.createNote.visibility = View.VISIBLE
            }

            is NoteListState.Blocked -> {
                println("⏳ Fragment showing Blocked")
                isEncrypted = true

                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.VISIBLE
                binding.createNote.visibility = View.GONE
                binding.recyclerViewList.visibility = View.GONE
                binding.recyclerViewPlanner.visibility = View.GONE
                btnLockIndicator.setImageResource(R.drawable.ic_locked)
                btnLockIndicator.visibility = View.VISIBLE
                //showAuthenticationDialog()
            }

            is NoteListState.Error -> {
                println("❌ Fragment showing error: ${state.message}")
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.GONE
                binding.recyclerViewList.visibility = View.GONE
                binding.recyclerViewPlanner.visibility = View.GONE
                btnLockIndicator.visibility = View.GONE
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
                btnLockIndicator.visibility = View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadNotes()
    }

    override fun onPause() {
        super.onPause()
        viewModel.onNotebookExited(navigateToNote)
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.onNotebookExited(navigateToNote)
    }
}