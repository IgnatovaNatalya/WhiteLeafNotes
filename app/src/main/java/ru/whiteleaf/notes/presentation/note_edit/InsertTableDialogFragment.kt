package ru.whiteleaf.notes.presentation.note_edit

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import ru.whiteleaf.notes.R

/**
 * Диалог для вставки таблицы
 */
class InsertTableDialogFragment : DialogFragment() {

    interface Listener {
        fun onTableInserted(rows: Int, cols: Int)
        fun onCanceled()
    }

    private lateinit var listener: Listener

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireActivity())
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_insert_table, null)

        val rowsInput = view.findViewById<EditText>(R.id.dialog_rows_input)
        val colsInput = view.findViewById<EditText>(R.id.dialog_cols_input)

        rowsInput.setText("3")
        colsInput.setText("3")

        builder.setView(view)
            .setTitle("Вставить таблицу")
            .setPositiveButton("Вставить") { _, _ ->
                val rows = rowsInput.text.toString().toIntOrNull() ?: 3
                val cols = colsInput.text.toString().toIntOrNull() ?: 3
                if (rows in 1..50 && cols in 1..50) {
                    listener.onTableInserted(rows, cols)
                }
            }
            .setNegativeButton("Отмена", null)

        return builder.create()
    }

    fun setListener(listener: Listener) {
        this.listener = listener
    }

    override fun onResume() {
        super.onResume()
        // Закрыть диалог при тапе вне его
        dialog?.setCanceledOnTouchOutside(true)
    }
}
