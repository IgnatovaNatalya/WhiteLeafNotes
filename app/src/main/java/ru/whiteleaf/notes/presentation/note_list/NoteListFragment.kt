package ru.whiteleaf.notes.presentation.note_list

import android.content.Intent
import androidx.biometric.BiometricPrompt
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
import ru.whiteleaf.notes.domain.model.BiometricRequest
import ru.whiteleaf.notes.presentation.state.NavigationEvent
import ru.whiteleaf.notes.presentation.state.NoteListState


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        viewModel.noteListState.observe(viewLifecycleOwner) { state -> renderState(state) }

        viewModel.navigationEvent.observe(viewLifecycleOwner) { event -> navigateEvent(event) }

        viewModel.biometricRequest.observe(viewLifecycleOwner) { request ->
            request?.let { showBiometricPrompt(it) }
        }

        viewModel.message.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearMessage()
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
        optionsButton?.setOnClickListener {
            ContextMenuHelper.showPopupMenu(
                context = requireContext(),
                anchorView = optionsButton,
                items = ContextMenuHelper.getOptionsMenuItemsNoteList(optionsButton.context, true),
                //todo определение зашифрована ли она
                onItemSelected = { itemId ->
                    when (itemId) {
                        R.id.options_create_note -> onOptionsCreateNote()
                        R.id.options_rename_notebook -> onOptionsRenameNotebook()
                        R.id.options_protect_notebook -> omOptionsProtectNotebook()
                        R.id.options_protect_notebook -> omOptionsUnProtectNotebook()
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

    private fun omOptionsProtectNotebook() = viewModel.protectNotebook(notebookTitle)

    private fun omOptionsUnProtectNotebook() = viewModel.unprotectNotebook(notebookTitle)

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

    private fun showBiometricPrompt(request: BiometricRequest) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Доступ к защищенным заметкам")
            .setSubtitle("Подтвердите отпечаток пальца")
            .setNegativeButtonText("Отмена")
            .build()

        val biometricPrompt =
            BiometricPrompt(this, object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    viewModel.reloadNotes()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    Toast.makeText(requireContext(), "Аутентификация отменена", Toast.LENGTH_SHORT)
                        .show()
                }
            })

        val cryptoObject = BiometricPrompt.CryptoObject(request.cipher)
        biometricPrompt.authenticate(promptInfo, cryptoObject)
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
        viewModel.onNavigated()
        findNavController().navigate(action)
    }

    private fun navigateToNotebook(path: String) {
        val action = NoteListFragmentDirections.actionGlobalNoteListFragment(path)
        findNavController().navigate(action)
    }

//    private fun navigateToNoteCreated(noteId: String) {
//        val action = NoteListFragmentDirections.actionGlobalNoteEditFragment(
//            noteId = noteId,
//            notebookPath = args.notebookPath
//        )
//        findNavController().navigate(action)
//    }

    private fun renderState(state: NoteListState) {
        when (state) {
            NoteListState.Blocked -> {
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.VISIBLE
                binding.createNote.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
            }

            is NoteListState.Error -> {
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.VISIBLE
                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE

                binding.emptyList.text = state.message
            }

            NoteListState.Loading -> {
                binding.noteListProgressBar.visibility = View.VISIBLE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.GONE
                binding.recyclerView.visibility = View.GONE
            }

            is NoteListState.Success -> {
                binding.noteListProgressBar.visibility = View.GONE
                binding.emptyList.visibility = View.GONE
                binding.notebookProtected.visibility = View.GONE
                binding.createNote.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.VISIBLE

                (binding.recyclerView.adapter as NoteAdapter).submitList(state.notes)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadNotes()
    }
}