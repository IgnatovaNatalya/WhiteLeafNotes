package ru.whiteleaf.notes.presentation.note_edit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.whiteleaf.notes.R

/**
 * Диалог для редактирования ячейки таблицы
 */
class TableCellEditDialogFragment : DialogFragment() {

    interface Listener {
        fun onCellSaved(text: String)
        fun onCanceled()
    }

    private lateinit var listener: Listener
    private lateinit var currentText: String

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_table_cell_edit, null)

        val editText = view.findViewById<EditText>(R.id.dialog_cell_edit_text)
        currentText = arguments?.getString(ARG_TEXT, "") ?: ""
        editText.setText(currentText)
        editText.requestFocus()

        builder.setView(view)
            .setTitle("Редактировать ячейку")
            .setPositiveButton("Сохранить") { _, _ ->
                listener.onCellSaved(editText.text.toString())
            }
            .setNegativeButton("Отмена", null)

        return builder.create()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    companion object {
        private const val ARG_TEXT = "cell_text"

        fun newInstance(text: String): TableCellEditDialogFragment {
            return TableCellEditDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TEXT, text)
                }
            }
        }
    }
}
