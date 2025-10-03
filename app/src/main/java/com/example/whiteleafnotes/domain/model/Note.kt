package com.example.whiteleafnotes.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note(
    val id:String, //Имя файла без .txt (техническое имя с _)
    val title: String, // Имя заметки, пустое если не задано
    val content: String,
    val modifiedAt: Long, // Timestamp
    val notebookPath: String? = null // Путь к папке относительно корня
) : Parcelable