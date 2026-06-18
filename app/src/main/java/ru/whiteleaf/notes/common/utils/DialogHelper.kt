package ru.whiteleaf.notes.common.utils

import android.app.AlertDialog as AndroidAlertDialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import ru.whiteleaf.notes.R

object DialogHelper {
    //notes
    fun createMoveNoteDialog(
        context: Context,
        onMoveClicked: (String) -> Unit
    ): AndroidAlertDialog {
        val alertDialogBuilder = AndroidAlertDialog.Builder(context)
        val moveDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_note_move, null)
        alertDialogBuilder.setView(moveDialogView)

        val newNotebook = moveDialogView.findViewById<EditText>(R.id.new_note_notebook)

        return alertDialogBuilder
            .setPositiveButton("Переместить") { _, _ ->
                onMoveClicked(newNotebook.text.toString())
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun createDeleteNoteConfirmationDialog(
        context: Context,
        noteTitle: String,
        onDeleteConfirmed: () -> Unit
    ): AndroidAlertDialog {
        val alertDialogBuilder = AndroidAlertDialog.Builder(context)
        val deleteDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        alertDialogBuilder.setView(deleteDialogView)

        val dialogTitle = deleteDialogView.findViewById<TextView>(R.id.delete_title)
        dialogTitle.text = "Удаление заметки"
        val dialogMessage = deleteDialogView.findViewById<TextView>(R.id.delete_message)
        val text =
            "Вы уверены, что хотите удалить заметку \"$noteTitle\"?"
        dialogMessage.text = text

        return alertDialogBuilder
            .setPositiveButton("Удалить") { _, _ -> onDeleteConfirmed() }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun createRenameNoteDialog(
        context: Context,
        currentTitle: String,
        onRenameConfirmed: (String) -> Unit
    ): AndroidAlertDialog {
        val alertDialogBuilder = AndroidAlertDialog.Builder(context)
        val renameDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        alertDialogBuilder.setView(renameDialogView)
        val dialogTitle = renameDialogView.findViewById<TextView>(R.id.rename_title)
        dialogTitle.text = "Переименование заметки"
        val newTitleEditText = renameDialogView.findViewById<EditText>(R.id.new_title)
        newTitleEditText.setText(currentTitle)
        newTitleEditText.selectAll()

        return alertDialogBuilder
            .setPositiveButton("Переименовать") { _, _ ->
                onRenameConfirmed(newTitleEditText.text.toString())
            }
            .setNegativeButton("Отмена", null)
            .create()
    }

//notebooks

    fun createDeleteNotebookDialog(
        context: Context,
        notebookTitle: String,
        onDeleteConfirmed: () -> Unit
    ): AndroidAlertDialog {
        val alertDialogBuilder = AndroidAlertDialog.Builder(context)
        val deleteDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        alertDialogBuilder.setView(deleteDialogView)

        val dialogTitle = deleteDialogView.findViewById<TextView>(R.id.delete_title)
        dialogTitle.text = "Удаление записной книжки"
        val dialogMessage = deleteDialogView.findViewById<TextView>(R.id.delete_message)
        val text =
            "Вы уверены, что хотите удалить записную книжку  \"$notebookTitle\" и все заметки в ней?"
        dialogMessage.text = text

        return alertDialogBuilder
            .setPositiveButton("Удалить") { _, _ -> onDeleteConfirmed() }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun createCreateNotebookDialog1(
        context: Context,
        onCreateConfirmed: (String) -> Unit
    )
            : AndroidAlertDialog {
        val alertDialogBuilder = AndroidAlertDialog.Builder(context)
        val createDialogView =
            LayoutInflater.from(context).inflate(R.layout.dialog_create_notebook, null)
        alertDialogBuilder.setView(createDialogView)

        val notebookName = createDialogView.findViewById<EditText>(R.id.notebook_name)

        return alertDialogBuilder
            .setPositiveButton("Создать") { _, _ -> onCreateConfirmed(notebookName.text.toString()) }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun createCreateNotebookDialog(
        context: Context,
        onCreateConfirmed: (String) -> Unit
    ): AlertDialog {

        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_create_notebook, null)
        builder.setView(view)

        val editText = view.findViewById<EditText>(R.id.notebook_name)

        val chips = listOf(
            view.findViewById<Chip>(R.id.chip_work),
            view.findViewById(R.id.chip_study),
            view.findViewById(R.id.chip_personal),
        )

        chips.forEach { chip ->
            chip.setOnClickListener {
                // Заполняем поле текстом чипа
                editText.setText(chip.text)
                // Устанавливаем курсор в конец
                editText.setSelection(editText.text?.length ?: 0)
                // Делаем чип выбранным (если нужно, чтобы он подсвечивался)
                // Для ChipGroup с singleSelection=false можно вручную управлять checked
                //chip.isChecked = true
                // Сбрасываем другие чипы (если хотим единственный выбор)
                //chips.filter { it != chip }.forEach { it.isChecked = false }
            }
        }

        val dialog = builder
            .setPositiveButton("Создать", null) // пока null, переопределим позже
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positiveButton.isEnabled = false

            editText.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    positiveButton.isEnabled = !s.isNullOrBlank()
                }
            })

            positiveButton.setOnClickListener {
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    onCreateConfirmed(name)
                    dialog.dismiss()
                }
            }

            // Автоматически показываем клавиатуру и фокусируем поле
            editText.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        return dialog
    }

    fun createRenameNotebookDialog(
        context: Context,
        currentName: String,
        onRenameConfirmed: (String) -> Unit
    ): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val renameDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        alertDialogBuilder.setView(renameDialogView)
        val dialogTitle = renameDialogView.findViewById<TextView>(R.id.rename_title)
        dialogTitle.text = "Переименование записной книжки"
        val newTitleEditText = renameDialogView.findViewById<EditText>(R.id.new_title)
        newTitleEditText.setText(currentName)
        newTitleEditText.selectAll()

        return alertDialogBuilder
            .setPositiveButton("Переименовать") { _, _ ->
                onRenameConfirmed(newTitleEditText.text.toString())
            }
            .setNegativeButton("Отмена", null)
            .create()
    }
}
