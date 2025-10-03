package com.example.whiteleafnotes.common.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.whiteleafnotes.domain.model.Note
import com.example.whiteleafnotes.domain.model.Notebook

object ShareHelper {

    fun shareNote(context: Context, note: Note, chooserTitle: String = "Поделиться заметкой") {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, "${note.title} \n\n${note.content}")
            type = "text/plain"
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    fun shareFile(context: Context, uri: Uri?, chooserTitle: String = "Поделиться файлом") {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, uri)
            type = "application/*"
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    fun shareNotebook(
        context: Context,
        notebook: Notebook,
        chooserTitle: String = "Поделиться записной книжкой"
    ) {
        //val shareIntent =
        //создать uri через ExternalRepository
    }
}