package com.example.txtnotesapp.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Note(
    val title: String, // Имя файла без .txt
    val content: String,
    val createdAt: Long, // Timestamp
    val notebookPath: String? = null // Путь к папке относительно корня
) : Parcelable