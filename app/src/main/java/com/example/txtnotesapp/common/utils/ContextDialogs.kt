package com.example.txtnotesapp.common.utils

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import com.example.txtnotesapp.R
import com.example.txtnotesapp.domain.model.Note

object DialogHelper {

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

    fun createDeleteNotebookConfirmationDialog(
        context: Context,
        notebookTitle: String,
        onDeleteConfirmed: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle("Удаление записной книжки")
            .setMessage("Вы уверены, что хотите удалить записную книжку  \"$notebookTitle\" и все заметки в ней?")
            .setPositiveButton("Удалить") { _, _ -> onDeleteConfirmed() }
            .setNegativeButton("Отмена", null)
            .create()
    }



    fun createDeleteNoteConfirmationDialog(
        context: Context,
        noteTitle: String,
        onDeleteConfirmed: () -> Unit
    ): AlertDialog {
        return AlertDialog.Builder(context)
            .setTitle("Удаление заметки")
            .setMessage("Вы уверены, что хотите удалить заметку \"$noteTitle\"?")
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

    fun createRenameNotebookDialog(
        context: Context,
        currentName: String,
        onRenameConfirmed: (String) -> Unit
    ): AlertDialog {
        val alertDialogBuilder = AlertDialog.Builder(context)
        val renameDialogView: View =
            LayoutInflater.from(context).inflate(R.layout.dialog_rename, null)
        alertDialogBuilder.setView(renameDialogView)
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
