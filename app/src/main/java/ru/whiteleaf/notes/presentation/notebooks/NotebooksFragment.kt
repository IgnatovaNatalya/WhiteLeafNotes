package ru.whiteleaf.notes.presentation.notebooks

import android.view.LayoutInflater
import android.view.ViewGroup
import ru.whiteleaf.notes.common.classes.BindingFragment
import ru.whiteleaf.notes.common.interfaces.ContextNotebookActionHandler
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

    override fun onDeleteNotebook(notebook: Notebook) {

    }

    override fun onRenameNotebook(notebook: Notebook) {
    }

    override fun onShareNotebook(notebook: Notebook) {
    }


//    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//
//    }

}