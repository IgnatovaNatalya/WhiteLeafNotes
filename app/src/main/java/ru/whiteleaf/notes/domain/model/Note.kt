package ru.whiteleaf.notes.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note(
    val id:String, //Имя файла без .txt (техническое имя с _)
    val title: String, // Имя заметки, пустое если не задано
    val content: String,
    val modifiedAt: Long, // Timestamp
    val notebookPath: String? = null // Путь к папке относительно корня
) : Parcelable {
    fun isEmpty(): Boolean =
        (this.title.trim().isEmpty() && this.content.trim().isEmpty())

    fun isNotEmpty() :Boolean =
        (this.title.trim().isNotEmpty() || this.content.trim().isNotEmpty())
}