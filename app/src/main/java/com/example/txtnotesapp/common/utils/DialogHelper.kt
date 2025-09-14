package com.example.txtnotesapp.common.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.Note

object DialogHelper {
//notes
    fun createMoveNoteDialog(
        context: Context,
        onMoveClicked: (String) -> Unit
    ): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
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
    ): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val deleteDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_delete, null)
        alertDialogBuilder.setView(deleteDialogView)

        val dialogTitle = deleteDialogView.findViewById<TextView>(R.id.delete_title)
        dialogTitle.text = "Удаление записной книжки"
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
    ): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
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
    ): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
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

    fun createCreateNotebookDialog(
        context: Context,
        onCreateConfirmed: (String) -> Unit)
    : AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val createDialogView = LayoutInflater.from(context).inflate(R.layout.dialog_create_notebook, null)
        alertDialogBuilder.setView(createDialogView)

        val notebookName = createDialogView.findViewById<EditText>(R.id.notebook_name)
        val name = notebookName.text.toString().trim()

        return alertDialogBuilder
            .setPositiveButton("Создать") { _, _ ->onCreateConfirmed(notebookName.text.toString())}
            .setNegativeButton("Отмена", null)
            .create()
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

    object ShareHelper {
        fun createShareNoteIntent(context: Context, note: Note): Intent {
            return Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, "${note.title} \n\n${note.content}")
                type = "text/plain"
            }
        }

        fun shareNote(context: Context, note: Note, chooserTitle: String = "Поделиться заметкой") {
            val shareIntent = createShareNoteIntent(context, note)
            context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
        }
    }
}
