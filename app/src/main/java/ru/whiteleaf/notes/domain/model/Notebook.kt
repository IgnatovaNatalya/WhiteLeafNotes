package ru.whiteleaf.notes.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Notebook(
    val path: String, // Путь папки в корневой папке заметок
    val createdAt: Long,
    val noteCount: Int =0,
    val isEncrypted: Boolean = false
) : Parcelable {
    val name = path
}