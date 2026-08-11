package ru.whiteleaf.notes.common.utils

import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SwitchCompat
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import ru.whiteleaf.notes.R
import ru.whiteleaf.notes.domain.model.Notebook

object DialogHelper {

    //export all notes
    fun createExportAllDialog(
        context: Context,
        pathToSave: String,
        numberEncrypted: Int = 0,
        onExportConfirmed: (Boolean, Boolean, String?) -> Unit
    ): AlertDialog {

        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_export_all, null)
        builder.setView(view)

        val tvPath = view.findViewById<TextView>(R.id.path_to_save)
        tvPath.text = pathToSave

        //Поделиться
        val swShareZip = view.findViewById<SwitchCompat>(R.id.sw_share_zip)

        //Экспортировать зашифрованные?
        val llExportEncrypted = view.findViewById<LinearLayout>(R.id.ll_export_encrypted)

        if (numberEncrypted == 0) {
            llExportEncrypted.visibility = View.GONE
        } else {
            val tvCountProtectedText = view.findViewById<TextView>(R.id.tv_export_encrypted_text)
            tvCountProtectedText.text = view.resources.getQuantityString(
                R.plurals.objects_required_count,
                numberEncrypted,
                numberEncrypted
            )
        }
        val swExportEncrypted = view.findViewById<SwitchCompat>(R.id.sw_export_encrypted)

        //пароль
        val swSetPassword = view.findViewById<SwitchCompat>(R.id.sw_set_password)
        val tilPassword = view.findViewById<TextInputLayout>(R.id.til_password)
        val etPassword = view.findViewById<EditText>(R.id.et_password)


        //делаем диалог
        val dialog = builder
            .setPositiveButton("Экспортировать", null) // пока null, переопределим позже
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {

            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            positiveButton.isEnabled = true

            swSetPassword.setOnCheckedChangeListener { _, isChecked ->

                if (isChecked) {
                    tilPassword.visibility = View.VISIBLE
                    positiveButton.isEnabled = false
                    etPassword.requestFocus()
                    val imm =
                        etPassword.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(etPassword, InputMethodManager.SHOW_IMPLICIT)
                } else {
                    etPassword.setText("")
                    tilPassword.visibility = View.GONE
                    positiveButton.isEnabled = true
                }
            }

            etPassword.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: android.text.Editable?) {
                    positiveButton.isEnabled = true
                    if (s.isNullOrBlank()) {
                        swSetPassword.isChecked = false
                        val imm =
                            etPassword.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(etPassword.windowToken, 0)
                        tilPassword.visibility = View.GONE
                    }
                }
            })

            positiveButton.setOnClickListener {
                val password = etPassword.text.toString().takeIf { it.isNotBlank() }
                onExportConfirmed(swShareZip.isChecked, swExportEncrypted.isChecked, password)
                dialog.dismiss()
            }
        }


        return dialog
    }

    //notes
    fun createMoveNoteDialog(
        context: Context,
        listNotebooks: List<Notebook>,
        currentNotebookPath: String,
        onMoveClicked: (String) -> Unit
    ): AlertDialog {

        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)

        val moveDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_note_move, null)
        builder.setView(moveDialogView)

        val autoComplete = moveDialogView.findViewById<AutoCompleteTextView>(R.id.new_note_notebook)

        val items = buildList {
            if (currentNotebookPath.isNotEmpty()) add(DropdownNotebookItem.RootItem)
            listNotebooks
                .filter { it.path != currentNotebookPath }
                .forEach { add(DropdownNotebookItem.NotebookItem(it)) }
        }

        val adapter = NotebookDropdownAdapter(context, items)
        autoComplete.setAdapter(adapter)

        var selectedItemForMove: DropdownNotebookItem? = null // храним выбранный элемент

        val dialog = builder
            .setPositiveButton("Переместить", null)
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.isEnabled = false

            autoComplete.setOnItemClickListener { parent, _, position, _ ->
                val item = parent.getItemAtPosition(position) as DropdownNotebookItem
                selectedItemForMove = item
                autoComplete.setText(item.toDisplayString())
                positiveButton.text = item.toLabelString()
                positiveButton.isEnabled = true
            }

            autoComplete.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: android.text.Editable?) {
                    val text = s.toString().trim()
                    if (text.isEmpty()) {
                        positiveButton.text = "Переместить"
                        positiveButton.isEnabled = false
                        selectedItemForMove = null
                    } else {
                        if (selectedItemForMove == null) {
                            positiveButton.text = "Создать и переместить"
                            positiveButton.isEnabled = true
                        }
                    }
                }
            })

            positiveButton.setOnClickListener {
                val text = autoComplete.text.toString().trim()
                when (selectedItemForMove) {
                    is DropdownNotebookItem.RootItem -> onMoveClicked("")  // корень
                    is DropdownNotebookItem.NotebookItem -> {
                        onMoveClicked((selectedItemForMove as DropdownNotebookItem.NotebookItem).notebook.path)
                    }

                    else -> onMoveClicked(text) // ручной ввод (создание нового)
                }
                dialog.dismiss()
            }
        }
        return dialog
    }

    fun createDeleteNoteConfirmationDialog(
        context: Context,
        noteTitle: String,
        onDeleteConfirmed: () -> Unit
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)
        val deleteDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        builder.setView(deleteDialogView)

        val dialogTitle = deleteDialogView.findViewById<TextView>(R.id.delete_title)
        val dialogMessage = deleteDialogView.findViewById<TextView>(R.id.delete_message)
        dialogTitle.text = "Удаление заметки"
        dialogMessage.text = "Вы уверены, что хотите удалить заметку «$noteTitle»?"

        return builder
            .setPositiveButton("Удалить") { _, _ -> onDeleteConfirmed() }
            .setNegativeButton("Отмена", null)
            .create()
    }

    fun createRenameNoteDialog(
        context: Context,
        currentTitle: String,
        onRenameConfirmed: (String) -> Unit
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)
        val renameDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        builder.setView(renameDialogView)

        val dialogTitle = renameDialogView.findViewById<TextView>(R.id.rename_title)
        dialogTitle.text = "Переименование заметки"

        val newTitleEditText = renameDialogView.findViewById<EditText>(R.id.new_title)
        newTitleEditText.setText(currentTitle)
        newTitleEditText.setSelection(currentTitle.lastIndex)

        val dialog = builder
            .setPositiveButton("Переименовать") { _, _ ->
                onRenameConfirmed(newTitleEditText.text.toString())
            }
            .setNegativeButton("Отмена", null)
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)

            newTitleEditText.addTextChangedListener(object : android.text.TextWatcher {
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
        }
        return dialog
    }

//notebooks

    fun createDeleteNotebookDialog(
        context: Context,
        notebookTitle: String,
        onDeleteConfirmed: () -> Unit
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)
        val deleteDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        builder.setView(deleteDialogView)

        val dialogTitle = deleteDialogView.findViewById<TextView>(R.id.delete_title)
        val dialogMessage = deleteDialogView.findViewById<TextView>(R.id.delete_message)

        dialogTitle.text = "Удаление записной книжки"
        dialogMessage.text =
            "Вы уверены, что хотите удалить записную книжку  «$notebookTitle» и все заметки в ней?"

        return builder
            .setPositiveButton("Удалить") { _, _ -> onDeleteConfirmed() }
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

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                }

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
            val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
        }

        return dialog
    }

    fun createRenameNotebookDialog(
        context: Context,
        currentName: String,
        onRenameConfirmed: (String) -> Unit
    ): AlertDialog {
        val builder = MaterialAlertDialogBuilder(context, R.style.WhiteLeafDialogTheme)
        val renameDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        builder.setView(renameDialogView)

        val dialogTitle = renameDialogView.findViewById<TextView>(R.id.rename_title)
        val newTitleEditText = renameDialogView.findViewById<EditText>(R.id.new_title)

        dialogTitle.text = "Переименование записной книжки"
        newTitleEditText.setText(currentName)
        newTitleEditText.selectAll()

        return builder
            .setPositiveButton("Переименовать") { _, _ ->
                onRenameConfirmed(newTitleEditText.text.toString())
            }
            .setNegativeButton("Отмена", null)
            .create()
    }
}
