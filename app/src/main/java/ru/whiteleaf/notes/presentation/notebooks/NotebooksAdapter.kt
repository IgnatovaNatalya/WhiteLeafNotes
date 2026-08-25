package ru.whiteleaf.notes.presentation.notebooks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import ru.whiteleaf.notes.common.interfaces.ContextNotebookActionHandler
import ru.whiteleaf.notes.databinding.ItemNotebooksNotebookBinding
import ru.whiteleaf.notes.domain.model.Notebook
import ru.whiteleaf.notes.presentation.start.NotebookViewHolder

class NotebooksAdapter (
    private val onNotebookClicked: (Notebook) -> Unit,
    private val contextActionHandler: ContextNotebookActionHandler
) : ListAdapter<Notebook, NotebookViewHolder>(NotebookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotebookViewHolder {
        val binding = ItemNotebooksNotebookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return NotebookViewHolder(binding.root, onNotebookClicked, contextActionHandler)
    }

    override fun onBindViewHolder(holder: NotebookViewHolder, position: Int) {
        val notebook = getItem(position)
        holder.bind(notebook)
    }

    class NotebookDiffCallback : DiffUtil.ItemCallback<Notebook>() {
        override fun areItemsTheSame(oldItem: Notebook, newItem: Notebook): Boolean {
            return oldItem.path == newItem.path
        }

        override fun areContentsTheSame(oldItem: Notebook, newItem: Notebook): Boolean {
            return oldItem == newItem
        }
    }
}