package ru.whiteleaf.notes.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notebook(
    val path: String, // Путь папки в корневой папке заметок
    val createdAt: Long,
    val noteCount: Int =0
) : Parcelable {
    val name = path
}