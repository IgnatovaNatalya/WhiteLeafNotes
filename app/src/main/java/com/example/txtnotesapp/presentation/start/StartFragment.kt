package com.example.txtnotesapp.presentation.start

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.txtnotesapp.R
import com.example.txtnotesapp.databinding.FragmentStartBinding
import com.example.txtnotesapp.domain.model.Note
import com.example.txtnotesapp.domain.model.Notebook
import org.koin.androidx.viewmodel.ext.android.viewModel

class StartFragment : Fragment() { //todo Сделать все фрагменты через BindingFragment
    private lateinit var binding: FragmentStartBinding
    private val viewModel: StartViewModel by viewModel()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()

        // Скрываем кнопку гамбургера в MainActivity
        //(activity as RootActivity).setDrawerEnabled(false)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Восстанавливаем кнопку гамбургера при выходе
        //(activity as RootActivity).setDrawerEnabled(true)
    }

    private fun setupRecyclerView() {
        val adapter = StartAdapter(
            onNotebookClicked = { notebook ->
                navigateToNotebook(notebook)
            },
            onNoteClicked = { note ->
                navigateToNote(note)
            },
            onAddNotebookClicked = {
                showCreateNotebookDialog()
            },
            onAddNoteClicked = {
                createNewNote()
            }
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

        viewModel.message.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun navigateToNotebook(notebook: Notebook) {
        val action = StartFragmentDirections.actionStartFragmentToNoteListFragment(notebook.path)
        findNavController().navigate(action)
    }

    private fun navigateToNote(note: Note) {
        val action = StartFragmentDirections.actionStartFragmentToNoteEditFragment(
            noteTitle = note.title,
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
}
